package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;

public class Tieset {
	int TiesetID;
	int reverse_tieset_id;
	List<Node> nodeList;
	List<Node> reverse_node_list;
	List<Node> ring_nodeList;
	List<Node> ring_reverse_nodeList;
	public Tieset(int TiesetID, List<Node> nodeList, int reverse_tieset_id, List<Node> reverse_node_list) {
		this.nodeList = nodeList;
		this.TiesetID = TiesetID;
		this.reverse_tieset_id = reverse_tieset_id;
		this.reverse_node_list = reverse_node_list;
		ring_nodeList = make_ring_nodeList(nodeList);
		ring_reverse_nodeList = make_ring_nodeList(reverse_node_list);
		//System.out.println(ring_nodeList);
		//System.out.println(ring_reverse_nodeList);
	}

	public int getTiesetID() {
		return TiesetID;
	}

	public void setTiesetID(int tiesetID) {
		TiesetID = tiesetID;
	}

	public List<Node> getNodeTable() {
		return nodeList;
	}

	public void setNodeTable(List<Node> nodeTable) {
		this.nodeList = nodeTable;
	}
	
	private List<Node> make_ring_nodeList(List<Node> nodeList){
		List<Node> ring_nodeList = new ArrayList<Node>();
		for(Node node : nodeList){
			ring_nodeList.add(node);
		}
		for(int i = 0; i < nodeList.size()-1; i++){
			ring_nodeList.add(nodeList.get(i));
		}
		return ring_nodeList;
	}

}
