package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Document;
import javax.xml.parsers.ParserConfigurationException;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;

public class BorderNodeFlow extends MakeFlow{
	MakeXML makexml = new MakeXML();
	BorderNodeFlow(List<Tieset> tiesetList, Graph<Node, Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph,Node[] globalNode, List<Integer> globalNode_ID) {
		this.tiesetList = tiesetList;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
		this.globalNode = globalNode;
		this.globalNode_ID = globalNode_ID;
	}

	
	//BorderNodeにフロー追加
	public void makeBorderFlow() throws ParserConfigurationException{
		List<Node> borderList = new ArrayList<Node>();
		borderList = makeBorderNodeList();
		
		for(Node node : borderList){
			int group_id = 1001;
			for(String IP : node.IPSet){
				//対象ノードど宛先ノードが一致した場合continueする
				if(node.node_id == node.host_node_map.get(IP).node_id){
					continue;
				}
				//nodeと宛先ノードのcontroller_idが一致（ローカルコントローラ内）
				if(node.host_node_map.get(IP).controller_id == node.controller_id){
					//スタート：対象境界ノード、　ゴール：エッジノード
					
					//最短経路メソッドの引数を変える
					//System.out.println("start:"+node+"goal"+node.host_node_map.get(IP));
					List<Node> local_shortest_node = findShortestPath(node, node.host_node_map.get(IP));
					
					int local_tieset_id= findNextTieset(local_shortest_node);
					
					Tieset local_target_tieset = findTieset(local_tieset_id);
					//順方向
					//タイセットID
					//local_tieset_id;
					//出力ポート
					int output_port = node.mapNextNode.get(local_shortest_node.get(1));
					
					//逆方向
					//タイセットIDからタイセットを算出
					boolean local_order = JudgeOrder(local_tieset_id);
					List<Node> local_reverseTiesetList = findReverseInfo(node, local_order, local_target_tieset);
					//System.out.println(reverseTiesetList);
					
					//逆方向のタイセットID取得
					int local_reverse_tieset_id = findReverseTiesetID(local_order, local_tieset_id);

					//逆方向の出力ポートの決定
					int local_reverese_output_port = findOutPutPort(node, local_reverseTiesetList);
					
					//MakeGroup
					int local_F_watch_port = output_port;
					int local_S_watch_port = local_reverese_output_port;
					int F_tieset_id = local_tieset_id;
					int S_tieset_id = local_reverse_tieset_id;
					makexml.BorderNodeGroup(node, group_id, local_F_watch_port, local_S_watch_port, F_tieset_id, S_tieset_id);
					
					//MakeFlow
					int priority = 200;
					makexml.localBorderNodeFlow(node, IP, group_id, priority);
					group_id++;
				}
				//宛先がローカルコントローラ外
				//スタート：エッジノードまたは境界ノード　ゴール：コントローラ内の中継ノード
				if(!(node.host_node_map.get(IP).controller_id == node.controller_id)){
					//コントローラ内で宛先コントローラに繋がる中継ノードへの最短経路と次タイセットを算出
					List<Node> borderEdgeList = new ArrayList<Node>();
					borderEdgeList = findBorderEdgeNode(node.controller_id, node.host_node_map.get(IP).controller_id);
					//System.out.println(borderEdgeList);
					
					
					//対象ノードが中継ノードなら判定しない
					if(!(borderEdgeList.contains(node))){
						//対象ノードから近い中継ノードを判定する
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
						//System.out.println(reverse_tieset_id);
						
						//MakeGroup
						int F_watch_port = output_port;
						int S_watch_port = reverese_output_port;
						int F_tieset_id = tieset_id;
						int S_tieset_id = reverse_tieset_id;
						makexml.BorderNodeGroup(node, group_id, F_watch_port, S_watch_port, F_tieset_id, S_tieset_id);
						
						//Postの作成（flow）
						int priority = 200;
						//IPからmplsを算出
						String mpls = String.valueOf(node.host_node_map.get(IP).controller_id);
						makexml.BorderNodeFlow(node, mpls, group_id, priority);
						group_id++;
					}
				}
			}
		}
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
	
	public Node findNearBorderNode(Node node, List<Node>borderEdgeList){
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

	private List<Node> makeBorderNodeList(){
		List<Node> borderList = new ArrayList<Node>();
		for(Node node : globalNode){
			if(node.BorderNode== true){
				//System.out.println(node.node_id);
				borderList.add(node);
			}
		}
		return borderList;
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
				//System.out.println("次ノード:"+reverseTiesetList.get(i+1).node_id);
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
