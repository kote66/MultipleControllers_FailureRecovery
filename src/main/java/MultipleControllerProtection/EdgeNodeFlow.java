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
	EdgeNodeFlow(List<Tieset>tiesetList, Node[] globalNode, Graph<Node,Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph) {
		this.tiesetList = tiesetList;
		this.globalNode = globalNode;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
	}

	//EdgeNodeにフロー追加（ホストから出力）
	public void makeEdgeFlow() throws ParserConfigurationException{
		List<Node> edgeList = new ArrayList<Node>();
		edgeList = makeEdgeNodeList();

		for(Node node : edgeList){
			int group_id = 801;
			for(String IP : Node.IPSet){
				//対象ノードど宛先ノードが一致した場合continueする
				if(node.node_id == node.host_node_map.get(IP).node_id){
					makeOutputHost(node, IP);
					continue;
				}
				//nodeと宛先ノードのcontroller_idが一致（ローカルコントローラ内）
				if(Node.host_node_map.get(IP).controller_id == node.controller_id){
					//スタート：対象境界ノード、 ゴール：エッジノード
					List<Node> local_shortest_node = findShortestPath(node, Node.host_node_map.get(IP));

					int local_tieset_id= findNextTieset(local_shortest_node);
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
					int local_S_watch_port = local_reverese_output_port;
					int local_F_tieset_id = local_tieset_id;
					int local_S_tieset_id = local_reverse_tieset_id;

					//MakeGroup
					makexml.local_EdgeNodeGroupPushVlan(node, group_id, local_F_watch_port, local_S_watch_port, local_F_tieset_id, local_S_tieset_id);

					//MakeFlow
					Set<String> hostList= node.Hostmap.keySet();
					String mac = Node.ChangeIP_toMac.get(IP);
					for(String host : hostList){
						int in_port = node.Hostmap.get(host);
						makexml.EdgeNodeflowPushVlan(node, mac, group_id, 200, in_port);

					}
					group_id++;

				}

				//宛先がローカルコントローラ外
				//スタート：エッジノードまたは境界ノード　ゴール：コントローラ内の中継ノード
				if(!(Node.host_node_map.get(IP).controller_id == node.controller_id)){
					//コントローラ内で宛先コントローラに繋がる中継ノードへの最短経路を算出
					List<Node> borderEdgeList = new ArrayList<Node>();
					borderEdgeList = findBorderEdgeNode(node.controller_id, Node.host_node_map.get(IP).controller_id);

					//対象ノードが中継ノードなら判定しない
					if(!(borderEdgeList.contains(node))){
						Node nearBorderEdgeNode = findNearBorderNode(node, borderEdgeList);
						List<Node> shortest_node = findShortestPath(node, nearBorderEdgeNode);
						int tieset_id= findNextTieset(shortest_node);

						//順方向か逆方向か判定
						boolean order = JudgeOrder(tieset_id);

						Tieset target_tieset = findTieset(tieset_id);

						//順方向の出力ポート
						int output_port = node.mapNextNode.get(shortest_node.get(1));


						//逆方向の情報を収集
						//タイセットIDからタイセットを算出
						List<Node> reverseTiesetList = findReverseInfo(node, order, target_tieset);

						//逆方向の出力ポートの決定
						int reverese_output_port = findOutPutPort(node, reverseTiesetList);

						//逆方向のタイセットID取得
						int reverse_tieset_id = findReverseTiesetID(order, tieset_id);

						int F_watch_port = output_port;
						int S_watch_port = reverese_output_port;
						int F_tieset_id = tieset_id;
						int S_tieset_id = reverse_tieset_id;

						//int in_port = node.Hostmap.get(IP);
						//接続しているホストのポート

						//MakeGroup
						int dst_controller = Node.host_node_map.get(IP).controller_id;
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
				if(Node.next_node_controller_id.values().contains(dst_controller_id)){
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
