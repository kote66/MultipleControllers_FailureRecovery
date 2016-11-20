package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import edu.uci.ics.jung.graph.Graph;

public class CoreNodeFlow  extends MakeFlow{

	CoreNodeFlow(List<Tieset>tiesetList, Node[] globalNode, Graph<Node,Integer> globalGraph, Graph<Integer, TiesetEdge> tiesetGraph) {
		this.tiesetList = tiesetList;
		this.globalNode = globalNode;
		this.globalGraph = globalGraph;
		this.tiesetGraph = tiesetGraph;
	}

	//BorderNodeにフロー追加
	public void CoreNodeFlow() throws ParserConfigurationException {
		List<Node> coreNodeList = new ArrayList<Node>();
		coreNodeList = makeCoreNodeList();

		for(Node node : coreNodeList){
			int group_id = 501;
			for(int controller_id : Node.controller_id_set){
				if(node.controller_id == controller_id){
					//コアノードのcontroller_idと宛先controller_idが一致したらflowの設定はしない
					continue;
				}

				int output_port = 0;
				//宛先コントローラに所属するコアノードへ転送する（下記の実装が済んだら必要なくなる）
				for(Node neighbor : node.neighborNode){
					if(neighbor.controller_id == controller_id){
						//出力ノード
						output_port = node.mapNextNode.get(neighbor);
					}
				}
				//未実装
				//アルゴリズム全てのローカルコアノードから最短距離のノードを選び、転送
				//最短経路を中継するノードにフローエントリを設定する
				//問題：ノード毎にフローエントリを設定しているため、他のノードへ設定することができない
				//経路を中継するノードにフローを設定し、コアノードで転送とIDの付与を行う
				int local_shortext_node_size = -1;
				List<Node> most_shortest_node = new ArrayList<Node>();
				for(Node dst_node : coreNodeList){
					List<Node> local_shortest_node = findShortestPath(node, dst_node);
					if(local_shortext_node_size < local_shortest_node.size()){
						most_shortest_node = local_shortest_node;
						local_shortext_node_size = local_shortest_node.size();
					}
				}
				System.out.println(most_shortest_node);
				int local_tieset_id= findNextTieset(most_shortest_node);

				Tieset local_target_tieset = findTieset(local_tieset_id);
				//次タイセット
				//出力ポートの決定
				
				
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
}
