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

	//RestAPIを使うためのクラスをインスタンス化
	MakeXML makexml = new MakeXML();
	MakeURI makeuri = new MakeURI();
	RestRequest postXML = new RestRequest();

	//最短経路の算出
	public List<Node> findShortestPath(Node start_node, Node goal_node){
		List<Node> shortest_node_path = new ArrayList<Node>();
		List<Integer> shortest_edge_path = new ArrayList<Integer>();

		DijkstraShortestPath<Node, Integer> dijkstra = new DijkstraShortestPath<Node, Integer>(globalGraph);
		shortest_edge_path = dijkstra.getPath(start_node, goal_node);

		//最短経路のエッジ情報をノード情報に変換
		shortest_node_path = edgeListChangeNodeList(shortest_edge_path, start_node, goal_node);

		return shortest_node_path;
	}

	//最短経路から次タイセットを算出
	public int findNextTieset(List<Node> shortest_node){
		int maxLength = -1;
		Tieset next_tieset = null;
		boolean order = false;

		for(Tieset tieset : tiesetList){
			int order_maxLength = -1;
			int reverse_maxLength = -1;

			order_maxLength = findMatchLengthTieset(tieset.ring_nodeList, shortest_node);
			reverse_maxLength = findMatchLengthTieset(tieset.ring_reverse_nodeList, shortest_node);

			if(maxLength < order_maxLength){
				maxLength = order_maxLength;
				next_tieset = tieset;
				order = true;
			}
			if(maxLength < reverse_maxLength){
				maxLength = reverse_maxLength;
				next_tieset = tieset;
				order = false;
			}
		}

		if(order == true){
			return next_tieset.TiesetID;
		}
		if(order == false){
			return next_tieset.reverse_tieset_id;
		}
		return 0;
	}

	public int get_flow_counter(){
		int flow_counter = Node.flow_counter;

		return flow_counter;
	}
	
	protected Tieset findTieset(int tieset_id){
		for(Tieset tieset : tiesetList){
			if(tieset.TiesetID == tieset_id || tieset.reverse_tieset_id == tieset_id){
				return tieset;
			}
		}
		return null;
	}

	//最短経路と一致度が一番高いタイセットを探す
	private int findMatchLengthTieset(List<Node> ring_nodeList, List<Node> shortest_node){
		int local_maxLength = -1;
		for(int i = 0; i < ring_nodeList.size(); i++){
			for(int j = 0; j < shortest_node.size(); j++){
				if(ring_nodeList.get(i+j).node_id == shortest_node.get(j).node_id){
					//一致度が一番高かったら更新する
					if(local_maxLength < j){
						local_maxLength = j;
					}
				}
				else{
					break;
				}
				//IndexOutの例外への対応
				if(i+j ==ring_nodeList.size()-1){
					break;
				}
			}
		}
		return local_maxLength;
	}

	private List<Node> edgeListChangeNodeList(List<Integer> shortest_edge_path, Node start_node, Node goal_node){
		List<Node> shortest_node_path = new ArrayList<Node>();
		for(Integer shortest_node: shortest_edge_path){
			//始点をリストの先頭にいれる
			if(globalGraph.getEndpoints(shortest_node).getSecond() == start_node){
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getSecond());
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getFirst());
			}
			else if(globalGraph.getEndpoints(shortest_node).getFirst() == start_node){
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getFirst());
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getSecond());
			}
			//終点をリストの最後に入れる
			else if(globalGraph.getEndpoints(shortest_node).getFirst() == goal_node){
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getFirst());	
			}
			else if(globalGraph.getEndpoints(shortest_node).getSecond() == goal_node){
				shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getSecond());
			}

			else{
				if(shortest_node_path.contains(globalGraph.getEndpoints(shortest_node).getFirst())){
					shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getSecond());
				}
				else if(shortest_node_path.contains(globalGraph.getEndpoints(shortest_node).getSecond())){
					shortest_node_path.add(globalGraph.getEndpoints(shortest_node).getFirst());
				}
			}
		}
		return shortest_node_path;
	}
}