package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;

public class TiesetEdge {
	int EdgeID;
	List<Integer> TiesetNode = new ArrayList<Integer>();

	TiesetEdge(int EdgeID) {
		this.EdgeID = EdgeID;
	}

	public void SetTiesetNode(int node1, int node2) {
		this.TiesetNode.add(node1);
		this.TiesetNode.add(node2);
	}
}
