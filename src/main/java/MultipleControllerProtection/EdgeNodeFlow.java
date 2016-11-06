package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.Element;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;

public class EdgeNodeFlow extends MakeFlow{
	MakeXML makexml = new MakeXML();
	static int edge_group_id = 4001;
	EdgeNodeFlow(List<Tieset> tiesetList, Graph<Node, Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph,Node[] globalNode, List<Integer> globalNode_ID) {
		this.tiesetList = tiesetList;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
		this.globalNode = globalNode;
		this.globalNode_ID = globalNode_ID;
	}

	
	//EdgeNodeにフロー追加（ホストから出力）
	public void makeEdgeFlow() throws ParserConfigurationException{
		List<Node> edgeList = new ArrayList<Node>();
		edgeList = makeEdgeNodeList();
		
		for(Node node : edgeList){
			int group_id = 801;
			for(String IP : node.IPSet){
				//対象ノードど宛先ノードが一致した場合continueする
				if(node.node_id == node.host_node_map.get(IP).node_id){
					 //System.out.println(node.node_id);
					 makeOutputHost(node, IP);
					continue;
				}
				//nodeと宛先ノードのcontroller_idが一致（ローカルコントローラ内）
				if(node.host_node_map.get(IP).controller_id == node.controller_id){
					//スタート：対象境界ノード、 ゴール：エッジノード
					List<Node> local_shortest_node = findShortestPath(node, node.host_node_map.get(IP));
					int local_tieset_id= findNextTieset(local_shortest_node);
					//System.out.println("tieset_id"+local_tieset_id);
					Tieset local_target_tieset = findTieset(local_tieset_id);
					
					//順方向への出力ポート
					int output_port = node.mapNextNode.get(local_shortest_node.get(1));

					//逆方向
					//タイセットIDからタイセットを算出
					boolean local_order = JudgeOrder(local_tieset_id);
					List<Node> local_reverseTiesetList = findReverseInfo(node, local_order, local_target_tieset);
					
					//逆方向のタイセットID取得
					int local_reverse_tieset_id = findReverseTiesetID(local_order, local_tieset_id);
					//逆方向の出力ポートの決定
					int local_reverese_output_port = findOutPutPort(node, local_reverseTiesetList);
					int local_F_watch_port = output_port;
					//System.out.println(local_F_watch_port);
					int local_S_watch_port = local_reverese_output_port;
					int local_F_tieset_id = local_tieset_id;
					int local_S_tieset_id = local_reverse_tieset_id;
					int mpls = node.host_node_map.get(IP).controller_id;
					String mpls_str = String.valueOf(mpls);
					
					//MakeGroup
					makexml.local_EdgeNodeGroupPushVlan(node, group_id, local_F_watch_port, local_S_watch_port, local_F_tieset_id, local_S_tieset_id);
					
					//MakeFlow
					Set<String> hostList= node.Hostmap.keySet();
					for(String host : hostList){
						int in_port = node.Hostmap.get(host);
						makexml.EdgeNodeflowPushVlan(node, IP, group_id, 200, in_port);
						
					}
					group_id++;

				}
				
				
				//宛先がローカルコントローラ外
				//スタート：エッジノードまたは境界ノード　ゴール：コントローラ内の中継ノード
				if(!(node.host_node_map.get(IP).controller_id == node.controller_id)){
					//境界ノードから仲介ノードまでの最短経路を算出し、次タイセットに入れる
					//コントローラ内で宛先コントローラに繋がる中継ノードへの最短経路を算出
					//まず宛先コントローラに繋がる中継ノードを探索する
					//引数に対象ノードのコントローラID, 宛先のコントローラID
					List<Node> borderEdgeList = new ArrayList<Node>();
					borderEdgeList = findBorderEdgeNode(node.controller_id, node.host_node_map.get(IP).controller_id);
					//System.out.println(borderEdgeList);
					
					//対象ノードから近い中継ノードを判定する
					//対象ノードが中継ノードなら判定しない
					
					if(!(borderEdgeList.contains(node))){
						Node nearBorderEdgeNode = findNearBorderNode(node, borderEdgeList);
						//System.out.println(node.node_id);
						//System.out.println(nearBorderEdgeNode.node_id);
						List<Node> shortest_node = findShortestPath(node, nearBorderEdgeNode);
						//System.out.println(shortest_node);
						int tieset_id= findNextTieset(shortest_node);
						//System.out.println(tieset_id);
						//順方向か逆方向か判定
						boolean order = JudgeOrder(tieset_id);
						//System.out.println(order);
						Tieset target_tieset = findTieset(tieset_id);
						//順方向のタイセットID, 次ノードはOK
						//次ノードの算出とタイセットIDの決定、出力ポートの決定を行う
						
						//順方向の出力ポート
						int output_port = node.mapNextNode.get(shortest_node.get(1));
						//System.out.println(output_port);
						
						//逆方向の情報を収集
						//タイセットIDからタイセットを算出
						List<Node> reverseTiesetList = findReverseInfo(node, order, target_tieset);
						//System.out.println(reverseTiesetList);
						
						//逆方向の出力ポートの決定
						int reverese_output_port = findOutPutPort(node, reverseTiesetList);
						//System.out.println(reverese_output_port);
						//逆方向のタイセットID取得
						int reverse_tieset_id = findReverseTiesetID(order, tieset_id);
						//System.out.println(reverse_tieset_id);
						
						int F_watch_port = output_port;
						int S_watch_port = reverese_output_port;
						int F_tieset_id = tieset_id;
						int S_tieset_id = reverse_tieset_id;
						
						//int in_port = node.Hostmap.get(IP);
						//接続しているホストのポート
						
						//MakeGroup
						int dst_controller = node.host_node_map.get(IP).controller_id;
						makexml.EdgeNodeGroupPushVlan(node, group_id, F_watch_port, S_watch_port, F_tieset_id, S_tieset_id, dst_controller);
						
						//接続しているホスト数だけ繰り返す
						Set<String> hostList= node.Hostmap.keySet();
						for(String host : hostList){
							int in_port = node.Hostmap.get(host);
							//MakeFlow
							makexml.EdgeNodeflowPushVlan(node, IP, group_id, 200, in_port);
						}
						group_id++;
					}
				}
			}
		}
	}
	
	//ホストへの出力
	public void makeOutputHost(Node node, String IP) throws ParserConfigurationException{
		//match:IP
		//vlanとmplsのタグを外す
		//接続先のホストに出力
		int output_port = node.Hostmap.get(IP);
		//flowの作成
		makexml.EdgeNodeGroupHost(node, IP, output_port , edge_group_id);
		makexml.EdgeNodeFlowHost(node, IP,output_port, 300, edge_group_id);
		edge_group_id++;
	}


	//宛先コントローラに接続している中継ノードを見つける
	private List<Node> findBorderEdgeNode(int local_controller_id, int dst_controller_id){
		List<Node> targetBorderEdgeList = new ArrayList<Node>();
		List<Node> borderEdgeList = new ArrayList<Node>();
		borderEdgeList = makeBorderEdgeNodeList();
		
		for(Node node : borderEdgeList){
			if(node.controller_id == local_controller_id){
				if(node.next_node_controller_id.values().contains(dst_controller_id)){
					targetBorderEdgeList.add(node);
				}
				
			}
		}
		return targetBorderEdgeList;
	}
	
	private Node findNearBorderNode(Node node, List<Node>borderEdgeList){
		int max_hop_count = 10000;
		Node nearBorderNode = null;
		for(Node borderEdgeNode : borderEdgeList){
			int hop_count = findShortestPath(node, borderEdgeNode).size();
			if( hop_count < max_hop_count){
				max_hop_count = hop_count;
				nearBorderNode = borderEdgeNode;
			}
		}
		return nearBorderNode;
	}
	
	/*
	//最短経路から経由するタイセットを算出
	private int findNextTieset(List<Node> targetList){
		int maxLength = -1;
		Tieset shortest_tieset = null;
		boolean order = false;
		for(Tieset tieset : tiesetList){
			int order_maxLength = -1;
			int reverse_maxLength = -1;
			//System.out.println("順方向");
			order_maxLength = judgeMatchLength(tieset.ring_nodeList, targetList, maxLength);
			//System.out.println(order_maxLength);
			//System.out.println("逆方向");
			reverse_maxLength = judgeMatchLength(tieset.ring_reverse_nodeList, targetList, maxLength);	
			//System.out.println(reverse_maxLength);
			//System.out.println(order_maxLength);
			//System.out.println(reverse_maxLength);
			if(maxLength < order_maxLength){
				maxLength = order_maxLength;
				shortest_tieset = tieset;
				order = true;
				//System.out.println("order"+tieset.ring_nodeList);
			}
			if(maxLength < reverse_maxLength){
				maxLength = reverse_maxLength;
				shortest_tieset = tieset;
				order = false;
				//System.out.println("reverse"+tieset.ring_reverse_nodeList);
			}
		}
	//System.out.println(maxLength);
	if(order == true){
		//System.out.println(shortest_tieset.nodeList);
		return shortest_tieset.TiesetID;
	}
	if(order == false){
		//System.out.println(shortest_tieset.reverse_node_list);
		return shortest_tieset.reverse_tieset_id;
	}
	
	//ゴールのノードが一致したタイセットに含まれるまで繰り返す。（次のタイセット算出までで十分）
	if(maxLength < targetList.size()-1){
		System.out.println("まだ次タイセットを算出する必要");
	}
	
	//一致度が高いタイセットが選ばれる。同じ一致度の場合は小さい方のタイセットが選ばれる
	//小さい方のタイセットを選ばせるには、更新時にタイセットの大きさも比較するとできる
	return 1000;
	}
*/

	private int judgeMatchLength(List<Node> ring_nodeList, List<Node> targetList, int maxLength){
		int local_maxLength = -1;
		for(int i = 0; i < ring_nodeList.size(); i++){
			for(int j = 0; j < targetList.size(); j++){
				if(ring_nodeList.get(i+j).node_id == targetList.get(j).node_id){
					//一致度が一番高かったら更新する
					if(maxLength < j){
						local_maxLength = j;
					}
				}
				else{
					break;
				}
				//IndexOutの例外対応
				if(ring_nodeList.get(i+j) == ring_nodeList.get(ring_nodeList.size()-1)){
					break;
				}
			}
		}
		return local_maxLength;
	}




	private List<Node> makeEdgeNodeList(){
		List<Node> edgeList = new ArrayList<Node>();
		for(Node node : globalNode){
			if(node.Edgenode == true){
				//System.out.println(node.node_id);
				edgeList.add(node);
			}
		}
		return edgeList;
	}
	/*
	private List<Node> makeEdgeNodeList(List<Node> borderList){
		for(Node node:globalNode){
			
		}
	}
	*/
	private List<Node> makeBorderEdgeNodeList(){
		List<Node> borderedgeList = new ArrayList<Node>();
		for(Node node :globalNode){
			if(node.CoreNode == true){
				//System.out.println(node.node_id);
				borderedgeList.add(node);
			}
		}
		return borderedgeList;
	}

	public void makeFlow(Node node, int group_id, int tieset_id) throws ParserConfigurationException{
		//String order_flow = makexml.tiesetflow(node.node_id, tieset.TiesetID);
		int priority = 100;
		makexml.tiesetflow(node, tieset_id, group_id, priority);
	}
/*
	//全体グラフによる最短経路の算出
	private List<Node> findShortestPath(Node start_node, Node goal_node){
		List<Integer> shortest_path = new ArrayList<Integer>();
		DijkstraShortestPath<Node, Integer> dijkstra = new DijkstraShortestPath<Node, Integer>(globalGraph);
		shortest_path = dijkstra.getPath(start_node, goal_node);
		//System.out.println("start_node:"+start_node);
		//System.out.println("goal_node:"+goal_node);
		//System.out.println("shortest_path:(Edge)"+shortest_path);

		List<Node> short_node = new ArrayList<Node>();

		for(Integer shortest_node: shortest_path){
			//始点をリストの先頭にいれる
			if(globalGraph.getEndpoints(shortest_node).getSecond() == start_node){
				short_node.add(globalGraph.getEndpoints(shortest_node).getSecond());
				short_node.add(globalGraph.getEndpoints(shortest_node).getFirst());
			}
			else if(globalGraph.getEndpoints(shortest_node).getFirst() == start_node){
				short_node.add(globalGraph.getEndpoints(shortest_node).getFirst());
				short_node.add(globalGraph.getEndpoints(shortest_node).getSecond());
			}
			//終点をリストの最後に入れる
			else if(globalGraph.getEndpoints(shortest_node).getFirst() == goal_node){
				short_node.add(globalGraph.getEndpoints(shortest_node).getFirst());	
			}
			else if(globalGraph.getEndpoints(shortest_node).getSecond() == goal_node){
				short_node.add(globalGraph.getEndpoints(shortest_node).getSecond());
			}

			else{
				if(short_node.contains(globalGraph.getEndpoints(shortest_node).getFirst())){
					short_node.add(globalGraph.getEndpoints(shortest_node).getSecond());
				}
				else if(short_node.contains(globalGraph.getEndpoints(shortest_node).getSecond())){
					short_node.add(globalGraph.getEndpoints(shortest_node).getFirst());
				}
			}
		}
		return short_node;
	}
	*/
	
	private boolean JudgeOrder(int tieset_id){
		boolean order = false;
		if(tieset_id < 2049){
			order = true;
		}
		return order;
	}
	
	private List<Node> findReverseInfo(Node node, boolean order, Tieset target_tieset) {
		if(order == true){
			//reverseを使う
			//System.out.println(target_tieset.reverse_tieset_id);
			return target_tieset.ring_reverse_nodeList;	
		}
		if(order == false){
			//orderを使う
			//System.out.println(target_tieset.TiesetID);
			return target_tieset.ring_nodeList;
		}
		return null;
	}
	
	private int findOutPutPort(Node node, List<Node>reverseTiesetList){
		Integer output_port = null;
		for(int i = 0 ; i < reverseTiesetList.size(); i++){
			if(node.node_id == reverseTiesetList.get(i).node_id){
				output_port = node.mapNextNode.get(reverseTiesetList.get(i+1));
				break;
			}
		}
		return output_port;
	}
	
	private int findReverseTiesetID(boolean order, int tieset_id){
		int reverse_tieset_id = 0;
		if(order == true){
			reverse_tieset_id = tieset_id + 2048;
			return reverse_tieset_id;
		}
		else{
			reverse_tieset_id = tieset_id - 2048;
			return reverse_tieset_id;
		}
	}
	
}
