package MultipleControllerProtection;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;


public class TiesetNodeFlow extends MakeFlow{
	MakeXML makexml = new MakeXML();
	
	TiesetNodeFlow(List<Tieset> tiesetList) {
		this.tiesetList = tiesetList;
		Node.flow_counter = 0;
	}
	
	//タイセットノードにフロー追加
	public void makeTiesetFlow() throws ParserConfigurationException{

		for(Tieset tieset : tiesetList){
			for(Node node : tieset.nodeList){
				makeGroup(node, tieset.TiesetID, tieset.nodeList);
				makeFlow(node, tieset.TiesetID, tieset.TiesetID);
			
			}
			
			for(Node re_node : tieset.reverse_node_list){
				makeGroup(re_node, tieset.reverse_tieset_id, tieset.reverse_node_list);
				makeFlow(re_node, tieset.reverse_tieset_id, tieset.reverse_tieset_id);
			}
		}
	}
	
	public void makeFlow(Node node, int group_id, int tieset_id) throws ParserConfigurationException{
			//String order_flow = makexml.tiesetflow(node.node_id, tieset.TiesetID);
			int priority = 150;		
			//System.out.println(node.node_id);
			makexml.tiesetflow(node, tieset_id, group_id, priority);
	}
	

	//GroupXMLの作り方
	public void makeGroup (Node node, int group_id, List<Node> tiesetList) throws ParserConfigurationException{
		List<Integer> watch_port = new ArrayList<Integer>();
		watch_port = findWatchPort(node, tiesetList);
		//System.out.println(tiesetList);
		//System.out.println(node.node_id);
		//System.out.println(watch_port);
		makexml.tiesetgroup(node, group_id, watch_port);
	}
	
	private List<Integer> findWatchPort(Node node, List<Node> tiesetList){
		//F_watch_port, S_watch_portなどを算出すれば、あとは引数に入れるだけでOK
		List<Integer> watch_port = new ArrayList<Integer>();
		int F_watch_port;
		int S_watch_port;
		int tieset_last = tiesetList.size()-1;		
		// タイセットの先頭
		if(node.node_id == tiesetList.get(0).node_id){
			// 順方向の出力ポート
			F_watch_port = node.mapNextNode.get(tiesetList.get(1));
			// 逆方向の出力ポート（最後尾を指定）
			S_watch_port = node.mapNextNode.get(tiesetList.get(tieset_last));
			//System.out.println("ノード"+node.node_id);
			//System.out.println("次ノード"+tiesetList.get(tieset_last));
		}
			
		//タイセットの最後尾
		else if(node.node_id == tiesetList.get(tieset_last).node_id){
			// 順方向の出力ポート（先頭を指定）
			F_watch_port = node.mapNextNode.get(tiesetList.get(0));
			// 逆方向の出力ポート
			S_watch_port = node.mapNextNode.get(tiesetList.get(tieset_last-1));
		}
			// 先頭と最後尾以外
		else{
			//タイセットリストにおけるノードの位置
			int point = tiesetList.indexOf(node);
			// 順方向の出力ポート
			F_watch_port = node.mapNextNode.get(tiesetList.get(point+1));
			// 逆方向の出力ポート
			S_watch_port = node.mapNextNode.get(tiesetList.get(point-1));
		}
		watch_port.add(F_watch_port);
		watch_port.add(S_watch_port);
		return watch_port;
	}
}
