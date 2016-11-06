package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TopologyInfo {
	Node[] node;
	List<Tieset> tiesetList = new ArrayList<Tieset>();
	Graph<Node, Integer> graph = new UndirectedSparseGraph<Node, Integer>();
	Graph<Integer, TiesetEdge> TiesetGraph = new UndirectedSparseGraph<Integer, TiesetEdge>();
	//Graph<String, Integer> graph = new UndirectedSparseGraph<String, Integer>();
	// Map<MacAddress, IPaddress>
	
	JsonNode rootNode;
	List<Integer> node_id_int = new ArrayList<Integer>();
	List<String> node_id = new ArrayList<String>();
	List<String> host_ip = new ArrayList<String>();
	List<String> host_mac = new ArrayList<String>();
	List<String> dst_node = new ArrayList<String>();
	List<String> source_node = new ArrayList<String>();
	List<String> dst_tp = new ArrayList<String>();
	List<String> source_tp = new ArrayList<String>();
	String host = "host";
	
	
	public Node SearchNode(int IntNodeId) {
		for (Node nodenum : node) {
			if (nodenum.node_id == IntNodeId) {
				return nodenum;
			}
		}
		return null;
	}
}
