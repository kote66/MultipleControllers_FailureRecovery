package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Node {
	int node_id;
	List<Integer> TiesetID = new ArrayList<Integer>();
	List<Node>  neighborNode = new ArrayList<Node>();
	List<String> HostID = new ArrayList<String>();
	static Map<String, String> ChangeIP_toMac = new HashMap<String, String>();
	static HashSet<String> IPSet = new HashSet<String>();
	static HashSet<Integer> controller_id_set = new HashSet<Integer>();
	static int flow_counter;
	boolean Edgenode;
	boolean BorderNode;
	boolean CoreNode;
	int controller_id;
	String belong_to_IP;

	// 出力ポートとNextNodeの紐付けOK
	Map<Node, Integer> mapNextNode = new LinkedHashMap<Node, Integer>();
	
	// HostNodeとポートの紐付け Map<IP, Port>
	Map<String, Integer> Hostmap = new HashMap<String, Integer>();
	
	//HostのIPをキー、接続しているノードを値としたMap<IP, node>
	static Map<String, Node> host_node_map = new HashMap<String, Node>();
	
	//ホストとコントローラIDの紐付けMap<IP, controller_id>
	Map<String, Integer> host_controller_map = new HashMap<String, Integer>();
	
	// Map<NodeId(openflow:),IPaddress>
	static Map<Integer, String> mapIP = new HashMap<Integer, String>();
	// Map<hostIP,TiesetID>
	Map<String, List<Integer>> TiesetIDtoIP = new HashMap<String, List<Integer>>();

	//次ノードをキー、次ノードのcontroller_idを値とするMap<NextNode, controller_id>
	static Map<Node, Integer> next_node_controller_id = new HashMap<Node, Integer>();
	
	// 境界ノードの場合：Map<キー:Dst_ip,値:TiesetID(vlanID), >
	

	
	
	
	Node(int node_id){
		this.node_id = node_id;
	}

	public Map<Node, Integer> getMap() {
		return this.mapNextNode;
	}

	public int extractNodeId(String NodeId) {
		String[] IntMynode = NodeId.split(":");
		Integer IntNodeId = Integer.parseInt(IntMynode[1]);
		return IntNodeId;
	}

	// BorderNode判定（タイセットに2つ以上所属しているノード）
	public void ifBorderNode() {
		if (1 < this.TiesetID.size()) {
			BorderNode = true;
		}
	}
	
	// EdgeNode判定
	public void ifEdgeNode(){
		
		
	}
	
	@Override
	public String toString() {
		String string_node_id=String.valueOf(node_id);
		return string_node_id;
	}
}