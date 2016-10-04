package MultipleControllerProtection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TopologyInfo {
	Node[] node;
	//Graph<Node, Integer> graph = new UndirectedSparseGraph<Node, Integer>();
	Graph<String, Integer> graph = new UndirectedSparseGraph<String, Integer>();
	// Map<MacAddress, IPaddress>
	JsonNode rootNode;
	List<String> node_id;
	List<String> host_ip;
	List<String> host_mac;
	List<String> dst_node;
	List<String> source_node;
	List<String> dst_tp;
	List<String> source_tp;
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
