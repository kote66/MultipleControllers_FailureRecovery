package MultipleControllerProtection;

import java.awt.Dimension;

import java.awt.font.LayoutPath;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.Renderer;

import com.fasterxml.jackson.core.TreeNode;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;


public class MakeTieset {
	Node[] globalNode;
	List<List<Node>> TiesetNodeList = new ArrayList<List<Node>>();
	Graph<Node, Integer> globalGraph = new UndirectedSparseGraph<Node, Integer>();
	Graph<Node, Integer> TreeGraph = new UndirectedSparseGraph<Node, Integer>();
	List<Tieset> tiesetList = new ArrayList<Tieset>();
	
	MakeTieset(Graph<Node, Integer> graph, Node[] globalNode) {
		this.globalGraph = graph;
		this.globalNode = globalNode;
		//次数が最大のノードを引数を入れる
		int node_id = findMaxOrder();
		BFS_TREE(findNodebyGraph(node_id));

		//補木を探す
		List<Pair<Node>> complement_tree = find_Complement_Tree(TreeGraph, globalGraph);

		//補木の両端ノードの最短経路探索
		DijkstraPath(complement_tree);
	}
	
	public void BFS_TREE(Node root) {
		Queue<Node> queue = new ArrayDeque<Node>();
	    //キューにrootを入れる
	    queue.clear();
	    queue.add(root);
	    int i = 0;
	    while(!queue.isEmpty()){
	    		//キューから取り除き、Treeに追加
	    		Node start_node =queue.remove();
	        TreeGraph.addVertex(start_node);
	        
	        //対象ノードの子をキューへ追加
	        	for(Node node:start_node.neighborNode){
	    	        //TreeGraphにneighborNodeが含まれていなければキューに追加
	        		if(!TreeGraph.containsVertex(node)){
	        			queue.add(node);
	        			TreeGraph.addEdge(i,start_node, node);
	        			i++;
	        		}
	        	}
	    }
	}
	
	private List<Pair<Node>> find_Complement_Tree(Graph<Node, Integer> TreeGraph, Graph<Node, Integer> globalGraph){
		List<Pair<Node>> complement_tree = new ArrayList<Pair<Node>>();
		for(int i = 0; i < globalGraph.getEdgeCount(); i++){
			Pair<Node> test= globalGraph.getEndpoints(i);
			for(int j = 0; j < TreeGraph.getEdgeCount(); j++){
				Pair<Node> test2= TreeGraph.getEndpoints(j);
				if(test.getFirst().node_id == test2.getFirst().node_id && test.getSecond().node_id == test2.getSecond().node_id){
					break;
				}
				if(test.getFirst().node_id == test2.getSecond().node_id && test.getSecond().node_id == test2.getFirst().node_id){
					break;
				}
				if(j == TreeGraph.getEdgeCount()-1){
					complement_tree.add(test);
				}
			}
		}
		return complement_tree;
	}
	
	//タイセットクラスに作成したタイセットを入れる
	private void DijkstraPath(List<Pair<Node>> complement_tree){
		List<Integer> shortest_path = new ArrayList<Integer>();
		DijkstraShortestPath<Node, Integer> dijkstra = new DijkstraShortestPath<Node, Integer>(TreeGraph);
		int tieset_id=1;
		for(Pair<Node> complement_edge :complement_tree){
			List<Node> tieset = new ArrayList<Node>();
			shortest_path = dijkstra.getPath(complement_edge.getFirst(), complement_edge.getSecond());
			//System.out.println("shortest_path"+shortest_path);
			for(Integer shortest_node: shortest_path){
				//System.out.println("shortest_node"+TreeGraph.getEndpoints(shortest_node));
				//始点をリストの先頭にいれる
				if(TreeGraph.getEndpoints(shortest_node).getSecond() == complement_edge.getFirst()){
					tieset.add(TreeGraph.getEndpoints(shortest_node).getSecond());
					tieset.add(TreeGraph.getEndpoints(shortest_node).getFirst());
				}
				//終点をリストの最後に入れる
				else if(TreeGraph.getEndpoints(shortest_node).getFirst() == complement_edge.getSecond()){
					tieset.add(TreeGraph.getEndpoints(shortest_node).getSecond());
					tieset.add(TreeGraph.getEndpoints(shortest_node).getFirst());
					break;
				}
				else{
					if(tieset.contains(TreeGraph.getEndpoints(shortest_node).getFirst())){
						tieset.add(TreeGraph.getEndpoints(shortest_node).getSecond());
					}
					else if(tieset.contains(TreeGraph.getEndpoints(shortest_node).getSecond())){
						tieset.add(TreeGraph.getEndpoints(shortest_node).getFirst());
					}
				}
			}
			TiesetNodeList.add(tieset);
			//タイセットの作成
			int reverse_tieset_id = tieset_id+2048;
			Tieset onetieset = new Tieset(tieset_id,tieset,reverse_tieset_id, ReverseList(tieset));
			tiesetList.add(onetieset);
			//System.out.println("タイセット"+i+":"+tieset);
			tieset_id++;
		}
		//System.out.println(TiesetNodeList);
	}
	
	private List<Node> ReverseList(List<Node> list) {
		List<Node> reverselist = new ArrayList<Node>();
		// System.out.println(list.size());
		for (int i = list.size() - 1; 0 <= i; i--) {
			// System.out.println("リストの要素"+list.get(i));
			reverselist.add(list.get(i));
		}
		// System.out.println("逆"+reverselist);
		return reverselist;
	}
	
	private Node findNodebyGraph(int node_id){
		for(Node node :globalGraph.getVertices()){
			//System.out.println(node.node_id);
				if (node.node_id == node_id) {
					return node;
				}
		}
		return null;
	}
	
	private int findMaxOrder(){
		int max_order = -1;
		Node max_node = null;
		for(Node node : globalNode){
			if(max_order < node.neighborNode.size()){
				max_order = node.neighborNode.size();
				max_node = node;
			}
		}
		return max_node.node_id;
	}
}
