package MultipleControllerProtection;

import java.util.List;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TiesetGraph {
	// タイセットをノードに接続関係をグラフとして表す
	Graph<Integer, TiesetEdge> TiesetGraph = new UndirectedSparseGraph<Integer, TiesetEdge>();

	// Node and TiesetID
	// Node and Next Node
	// タイセットIDをノードとする
	// 全てのノードの所属タイセットからタイセットパスグラフを作ればよいのでは？
	TiesetGraph() {

	}

	public void MakeTiesetGraph(List<Integer> TiesetID) {
		int n = TiesetID.size();
		if (n > 1) {
			for (int j = n; n - j < 1; j--) {
				int EdgeNumber = 1;
				for (int i = 1; i < n; i++) {
					TiesetEdge tiesetedge = new TiesetEdge(EdgeNumber);
					TiesetGraph.addEdge(tiesetedge, TiesetID.get(n - j), TiesetID.get(i));
					tiesetedge.SetTiesetNode(TiesetID.get(n - j), TiesetID.get(i));
					EdgeNumber++;
				}
			}
		}
		else if(n == 1){
			TiesetGraph.addVertex(TiesetID.get(0));
			//タイセットグラフがTopologyInfoではない
		}
		//TopologyInfoに代入
	}

	public Graph<Integer, TiesetEdge> getTiesetGraph() {
		return TiesetGraph;
	}
}
