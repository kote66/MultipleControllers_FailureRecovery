package MultipleControllerProtection;

import java.util.ArrayList;

import java.util.List;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class MakeFlow {

	Node[] globalNode;
	List<Tieset> tiesetList = new ArrayList<Tieset>();
	Graph<Node, Integer> globalGraph = new UndirectedSparseGraph<Node, Integer>();
	Graph<Integer, TiesetEdge> tiesetGraph = new UndirectedSparseGraph<Integer, TiesetEdge>();

	MakeXML makexml = new MakeXML();
	MakeURI makeuri = new MakeURI();
	RestRequest postXML = new RestRequest();

	//全体グラフによる最短経路の算出
	public List<Node> findShortestPath(Node start_node, Node goal_node){
		List<Integer> shortest_path = new ArrayList<Integer>();
		DijkstraShortestPath<Node, Integer> dijkstra = new DijkstraShortestPath<Node, Integer>(globalGraph);
		shortest_path = dijkstra.getPath(start_node, goal_node);

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


	//最短経路から経由するタイセットを算出
	public int findNextTieset(List<Node> targetList){
		int maxLength = -1;
		Tieset shortest_tieset = null;
		boolean order = false;
		for(Tieset tieset : tiesetList){
			int order_maxLength = -1;
			int reverse_maxLength = -1;
			order_maxLength = judgeMatchLength(tieset.ring_nodeList, targetList);
			reverse_maxLength = judgeMatchLength(tieset.ring_reverse_nodeList, targetList);
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
			return shortest_tieset.TiesetID;
		}
		if(order == false){
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

	private int judgeMatchLength(List<Node> ring_nodeList, List<Node> targetList){
		int local_maxLength = -1;
		for(int i = 0; i < ring_nodeList.size(); i++){
			for(int j = 0; j < targetList.size(); j++){
				if(ring_nodeList.get(i+j).node_id == targetList.get(j).node_id){
					//一致度が一番高かったら更新する
					if(local_maxLength < j){
						local_maxLength = j;
					}
				}
				else{
					break;
				}
				//IndexOutの例外対応
				if(i+j ==ring_nodeList.size()-1){
					break;
				}
			}
		}
		return local_maxLength;
	}

	protected Tieset findTieset(int tieset_id){
		for(Tieset tieset : tiesetList){
			if(tieset.TiesetID == tieset_id || tieset.reverse_tieset_id == tieset_id){
				return tieset;
			}
		}
		return null;
	}
	public int get_flow_counter(){
		int flow_counter = Node.flow_counter;
		
		return flow_counter;
	}
}