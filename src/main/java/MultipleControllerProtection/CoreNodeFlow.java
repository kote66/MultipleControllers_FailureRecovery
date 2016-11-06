package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import edu.uci.ics.jung.graph.Graph;

public class CoreNodeFlow  extends MakeFlow{

	CoreNodeFlow(List<Tieset> tiesetList, Graph<Node, Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph,Node[] globalNode, List<Integer> globalNode_ID) {
		this.tiesetList = tiesetList;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
		this.globalNode = globalNode;
		this.globalNode_ID = globalNode_ID;
	}

	//BorderNodeにフロー追加
	public void CoreNodeFlow() throws ParserConfigurationException{
		List<Node> coreNodeList = new ArrayList<Node>();
		coreNodeList = makeCoreNodeList();

		for(Node node : coreNodeList){
			int group_id = 501;
			for(int controller_id : node.controller_id_set){
				if(node.controller_id == controller_id){
					//コアノードのcontroller_idと宛先controller_idが一致したらflowの設定はしない
					continue;
				}

				int output_port = 0;
				//宛先コントローラに所属するコアノードへ転送する
				for(Node neighbor : node.neighborNode){
					if(neighbor.controller_id == controller_id){
						//出力ノード
						output_port = node.mapNextNode.get(neighbor);
					}
				}
				//未実装
				/*
				//迂回経路（ローカルコントローラ内で一番近いコアノードへの転送）
				//アルゴリズム全てのローカルコアノードから最短距離のノードを選び、転送
				List<Node> local_shortest_node = findShortestPath(node, neighbor_node);
				//System.out.println(local_shortest_node);
				int local_tieset_id= findNextTieset(local_shortest_node);

				Tieset local_target_tieset = findTieset(local_tieset_id);
				//次タイセット
				//出力ポートの決定
				 * 
				 */
				//MakeGroup
				int F_watch_port = output_port;
				//int S_watch_port = reverese_output_port;
				//int S_tieset_id = reverse_tieset_id;
				makexml.CoreNodeGroup(node, group_id, F_watch_port);
				
				//Postの作成（flow）
				int priority = 200;
				String mpls = String.valueOf(controller_id);
				makexml.CoreNodeFlow(node, mpls, group_id, priority);
				group_id++;
				
				//ループ障害への対応
				//MakeGroup
				//MakeFlow
				group_id++;
			}
		}	
	}
	private List<Node> makeCoreNodeList(){
		List<Node> coreNodeList = new ArrayList<Node>();
		for(Node node : globalNode){
			if(node.CoreNode== true){
				//System.out.println(node.node_id);
				coreNodeList.add(node);
			}
		}
		return coreNodeList;
	}
	
	public int get_flow_counter(){
		int flow_counter = globalNode[0].flow_counter;
		
		return flow_counter;
	}
}
