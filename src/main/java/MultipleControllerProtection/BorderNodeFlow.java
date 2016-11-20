package MultipleControllerProtection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import edu.uci.ics.jung.graph.Graph;

public class BorderNodeFlow extends MakeFlow{
	MakeXML makexml = new MakeXML();
	
	BorderNodeFlow(List<Tieset>tiesetList, Node[] globalNode, Graph<Node,Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph) {
		this.tiesetList = tiesetList;
		this.globalNode = globalNode;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
	}

	//BorderNodeにフロー追加
	public void makeBorderFlow() throws ParserConfigurationException{
		List<Node> borderList = new ArrayList<Node>();
		borderList = makeBorderNodeList();

		for(Node node : borderList){
			int group_id = 1001;
			for(String IP : Node.IPSet){
				//対象ノードど宛先ノードが一致した場合continueする
				if(node.node_id == Node.host_node_map.get(IP).node_id){
					continue;
				}
				//nodeと宛先ノードのcontroller_idが一致（ローカルコントローラ内）
				if(Node.host_node_map.get(IP).controller_id == node.controller_id){
					//次タイセットの取得
					List<Node> local_shortest_node = findShortestPath(node, Node.host_node_map.get(IP));
					int local_tieset_id= findNextTieset(local_shortest_node);
					
					//逆方向のタイセットID取得
					Tieset local_target_tieset = findTieset(local_tieset_id);

					//順方向への出力ポート
					int output_port = node.mapNextNode.get(local_shortest_node.get(1));

					//逆方向の場合
					boolean local_order = JudgeOrder(local_tieset_id);
					List<Node> local_reverseTiesetList = findReverseInfo(node, local_order, local_target_tieset);

					//逆方向のタイセットID取得
					int local_reverse_tieset_id = findReverseTiesetID(local_order, local_tieset_id);

					//逆方向への出力ポート
					int local_reverese_output_port = findOutPutPort(node, local_reverseTiesetList);

					//グループテーブルの作成
					int local_F_watch_port = output_port;
					int local_S_watch_port = local_reverese_output_port;
					int F_tieset_id = local_tieset_id;
					int S_tieset_id = local_reverse_tieset_id;
					makexml.BorderNodeGroup(node, group_id, local_F_watch_port, local_S_watch_port, F_tieset_id, S_tieset_id);

					//フローエントリの作成
					int priority = 200;
					makexml.localBorderNodeFlow(node, IP, group_id, priority);
					group_id++;

					//ループ障害対策のフローエントリを追加
					int in_port = local_F_watch_port;
					int tieset_id = S_tieset_id;
					int roop_priority = 400;
					makexml.BorderNodeFlow_loop(node, IP, in_port, tieset_id, roop_priority);
					group_id++;
				}
				//宛先ホストがローカルコントローラ外の場合
				if(!(Node.host_node_map.get(IP).controller_id == node.controller_id)){
					List<Node> borderEdgeList = new ArrayList<Node>();
					borderEdgeList = findBorderEdgeNode(node.controller_id, Node.host_node_map.get(IP).controller_id);

					//対象ノードが中継ノードなら判定しない
					if(!(borderEdgeList.contains(node))){
						//対象ノードから近い中継ノードを判定する
						Node nearBorderEdgeNode = findNearBorderNode(node, borderEdgeList);
						//コントローラ内で宛先コントローラに繋がるコアノードへの最短経路を算出
						List<Node> shortest_node = findShortestPath(node, nearBorderEdgeNode);
						//次タイセットの算出
						int tieset_id= findNextTieset(shortest_node);

						boolean order = JudgeOrder(tieset_id);
						Tieset target_tieset = findTieset(tieset_id);

						//順方向への出力ポート
						int output_port = node.mapNextNode.get(shortest_node.get(1));

						//逆方向の情報を収集
						//タイセットIDからタイセットを算出
						List<Node> reverseTiesetList = findReverseInfo(node, order, target_tieset);

						//逆方向への出力ポートの決定
						int reverese_output_port = findOutPutPort(node, reverseTiesetList);
						//逆方向のタイセットID取得
						int reverse_tieset_id = findReverseTiesetID(order, tieset_id);

						//グループテーブルの作成
						int F_watch_port = output_port;
						int S_watch_port = reverese_output_port;
						int F_tieset_id = tieset_id;
						int S_tieset_id = reverse_tieset_id;
						makexml.BorderNodeGroup(node, group_id, F_watch_port, S_watch_port, F_tieset_id, S_tieset_id);

						//フローエントリの作成
						int priority = 200;
						String mpls = String.valueOf(Node.host_node_map.get(IP).controller_id);
						makexml.BorderNodeFlow(node, mpls, group_id, priority);
						group_id++;

						//ループ障害対策のフローエントリを追加
						int in_port = F_watch_port;
						int roop_priority = 400;
						makexml.BorderNodeFlow_loop(node, IP, in_port, S_tieset_id, roop_priority);
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
				if(Node.next_node_controller_id.values().contains(dst_controller_id)){
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
				borderList.add(node);
			}
		}
		return borderList;
	}

	private List<Node> makeBorderEdgeNodeList(){
		List<Node> borderedgeList = new ArrayList<Node>();
		for(Node node :globalNode){
			if(node.CoreNode == true){
				borderedgeList.add(node);
			}
		}
		return borderedgeList;
	}

	public void makeFlow(Node node, int group_id, int tieset_id) throws ParserConfigurationException{
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
			return target_tieset.ring_reverse_nodeList;	
		}
		if(order == false){
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
