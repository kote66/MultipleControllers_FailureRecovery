package MultipleControllerProtection;

import java.io.IOException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class Topology {
	Node[] globalNode;
	List<Tieset> tiesetList = new ArrayList<Tieset>();
	Graph<Node, Integer> globalGraph = new UndirectedSparseGraph<Node, Integer>();
	Graph<Integer, TiesetEdge> TiesetGraph = new UndirectedSparseGraph<Integer, TiesetEdge>();
	ArrayList<Integer> NodeNextNode = new ArrayList<Integer>();
	Map<String, String> ChangeMac_toIP = new HashMap<String, String>();
	//Tieset[] tieset;
	//Tieset[] reversetieset;
	List<TopologyInfo> topologyInfo = new ArrayList<TopologyInfo>();
	//コントローラの数に応じてtestを変える
	TopologyInfo test1 = new TopologyInfo();
	//TopologyInfo test2 = new TopologyInfo();
	String host = "host";
	List<Integer> globalNode_ID = new ArrayList<Integer>();	
	List<String> global_src_tp = new ArrayList<String>();
	List<String> global_dst_tp = new ArrayList<String>();
	List<String> globalEdge = new ArrayList<String>();
	List<String> hostInfoList = new ArrayList<String>();

	Topology(List<String> jsonlist) {
		//コントローラの数に応じてtestを変える
		topologyInfo.add(test1);
		//topologyInfo.add(test2);
		for(int i = 0; i < jsonlist.size(); i++){
			ObjectMapper mapper = new ObjectMapper();
			try {
				topologyInfo.get(i).rootNode = (mapper.readTree(jsonlist.get(i)));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void getTopologyInfo() {
		for(int num = 0; num < topologyInfo.size(); num ++){
			Arrange(topologyInfo.get(num).rootNode, num);
			addGlobalNode(num);
		}
		removeRepetition();
		addNode();	
		addEdge(global_src_tp, global_dst_tp);
		setHostInfo();
		MakeTieset maketieset = new MakeTieset(globalGraph, globalNode);
		this.tiesetList =maketieset.tiesetList;
		addTiesetIDtoNode();
		MakeTiesetGraph();
		booleanBorder();
		set_controller_id();
		findBorderEdgeNode(globalNode);
	}

	public void test(){
		//System.out.println(globalNode_ID);
		//System.out.println(global_dst_tp);
		//System.out.println(global_src_tp);
		System.out.println("全体グラフ");
		System.out.println(globalGraph.toString());
		for(int i=0 ; i < tiesetList.size(); i++){
			System.out.println("タイセットID"+tiesetList.get(i).TiesetID+":"+tiesetList.get(i).nodeList);
			System.out.println("タイセットID"+tiesetList.get(i).reverse_tieset_id+":"+tiesetList.get(i).reverse_node_list);
		}
		System.out.println(TiesetGraph);
		//for(Node node :globalGraph.getVertices()){
		//System.out.println(node.node_id);
		//}
		//System.out.println(SearchNode(2).mapNextNode);
	}

	private void Arrange(JsonNode rootNode, int controller_id) {
		//各コントローラにトポロジ情報追加
		topologyInfo.get(controller_id).node_id = rootNode.findValuesAsText("node-id");
		topologyInfo.get(controller_id).node_id.remove("controller-config"); // ノード情報に不必要な情報を取り除く
		topologyInfo.get(controller_id).host_ip = rootNode.findValuesAsText("ip");
		topologyInfo.get(controller_id).host_mac = rootNode.findValuesAsText("mac");
		topologyInfo.get(controller_id).dst_node = rootNode.findValuesAsText("dest-node");
		topologyInfo.get(controller_id).source_node = rootNode.findValuesAsText("source-node");
		topologyInfo.get(controller_id).dst_tp = rootNode.findValuesAsText("dest-tp");
		topologyInfo.get(controller_id).source_tp = rootNode.findValuesAsText("source-tp");
		//全体のトポロジ情報の作成
		for(String dst_tp:rootNode.findValuesAsText("dest-tp")){
			global_dst_tp.add(dst_tp);
		}
		for(String src_tp:rootNode.findValuesAsText("source-tp")){
			global_src_tp.add(src_tp);
		}
		//macとIPの紐付け
		for(int i=0; i < topologyInfo.get(controller_id).host_mac.size();i++){
			ChangeMac_toIP.put("host:" + topologyInfo.get(controller_id).host_mac.get(i), topologyInfo.get(controller_id).host_ip.get(i));
		}
	}

	private void removeRepetition(){
		//不必要な情報を削除
		removeHost(global_src_tp, global_dst_tp);
		//重複しているエッジを削除
		removeEdgeRepetition(global_src_tp, global_dst_tp);
	}

	private void addNode() {
		globalNode = new Node[globalNode_ID.size()];

		for (int i = 0; i < globalNode_ID.size(); i++) {
			globalNode[i] = new Node(globalNode_ID.get(i));
			globalGraph.addVertex(globalNode[i]);
		}
	}

	private void addGlobalNode(int num){
		for (String mynode : topologyInfo.get(num).node_id) {
			if (!mynode.startsWith(host)) {
				String mynode2 = RemoveOpenFlow(mynode);
				Integer NodeId = Integer.parseInt(mynode2);
				globalNode_ID.add(NodeId);
			}
		}
	}

	private void addEdge(List<String> src_tp, List<String> dst_tp){
		for(int i = 0; i < src_tp.size(); i++){
			String[] src_Info = src_tp.get(i).split(":");
			String[] dst_Info = dst_tp.get(i).split(":");
			int src_node =Integer.parseInt(src_Info[1]);
			int dst_node =Integer.parseInt(dst_Info[1]);
			int src_node_tp =Integer.parseInt(src_Info[2]);
			int dst_node_tp =Integer.parseInt(dst_Info[2]);
			SearchNode(src_node).neighborNode.add(SearchNode(dst_node));
			SearchNode(dst_node).neighborNode.add(SearchNode(src_node));
			SearchNode(src_node).mapNextNode.put(SearchNode(dst_node), src_node_tp);
			SearchNode(dst_node).mapNextNode.put(SearchNode(src_node), dst_node_tp);
			globalGraph.addEdge(i, SearchNode(src_node),  SearchNode(dst_node));
		}
	}

	private Node SearchNode(int node_id) {
		for (Node nodenum : globalNode) {
			if (nodenum.node_id == node_id) {
				return nodenum;
			}
		}
		return null;
	}

	//"host"と対向ポートを取り除く＆エッジノードの情報を取得
	private void removeHost(List<String> src_tp, List<String> dst_tp){
		for(int i = 0; i < src_tp.size(); i++){
			if (src_tp.get(i).startsWith("host") || dst_tp.get(i).startsWith("host")) {
				//ホスト情報と接続関係を保持
				getHostInfo(src_tp.get(i), dst_tp.get(i));
				//ホストと対向ポートを削除
				src_tp.remove(i);
				dst_tp.remove(i);
				i--;
			}
		}
	}

	private void getHostInfo(String src_tp, String dst_tp){
		String IP = "";
		String node = "";
		String node_tp = "";
		if (src_tp.startsWith("host")){
			IP = ChangeMac_toIP.get(src_tp);
			String[] dst_Info = dst_tp.split(":");
			node = dst_Info[1];
			node_tp =dst_Info[2];
		}
		if (dst_tp.startsWith("host")){
			IP = ChangeMac_toIP.get(dst_tp);
			String[] src_Info = src_tp.split(":");
			node =src_Info[1];
			node_tp =src_Info[2];
		}
		hostInfoList.add(node);
		hostInfoList.add(IP);
		hostInfoList.add(node_tp);
	}
	//ホスト情報の追加＆エッジノード判定
	private void setHostInfo(){
		for(int i = 0; i < hostInfoList.size(); i++){
			//System.out.println(hostInfoList);
			int node = Integer.parseInt(hostInfoList.get(i));
			String IP = hostInfoList.get(++i);
			int node_tp = Integer.parseInt(hostInfoList.get(++i));
			SearchNode(node).Hostmap.put(IP, node_tp);
			SearchNode(node).host_node_map.put(IP, SearchNode(node));
			//Edgenode判定
			SearchNode(node).Edgenode = true;
			//ホストIPをリストで保持
			SearchNode(node).IPSet.add(IP);
			//System.out.println(node);
			//System.out.println(SearchNode(node).Hostmap);
		}
		//SearchNode(node);

	}

	private void removeEdgeRepetition(List<String> src_tp, List<String> dst_tp){
		for(int i = 0; i < src_tp.size(); i++){
			findEdgeRepetition(src_tp.get(i), dst_tp.get(i));
		}
	}

	private void findEdgeRepetition(String src_tp, String dst_tp){
		for(int i = 0; i < global_src_tp.size(); i++){
			if(src_tp.equals(global_dst_tp.get(i))&& src_tp.equals(global_dst_tp.get(i))){
				global_src_tp.remove(i);
				global_dst_tp.remove(i);
				i--;
			}
		}
	}

	private String RemoveOpenFlow(String Node) {
		String[] NodeInfo = Node.split(":");
		String NodeNum = NodeInfo[1];
		return NodeNum;
	}
	//ノードにタイセットIDをリストで追加
	private void addTiesetIDtoNode(){
		for(int i=0 ; i < tiesetList.size(); i++){
			//System.out.println("タイセットID"+i+":"+tiesetList.get(i).nodeList);
			for(Node node:tiesetList.get(i).nodeList){
				node.TiesetID.add(i);
			}
		}
	}

	//タイセットグラフの作成
	private void MakeTiesetGraph() {
		TiesetGraph tiesetgraph = new TiesetGraph();
		for (int i = 0; i < globalNode.length - 2; i++) {
			tiesetgraph.MakeTiesetGraph(globalNode[i].TiesetID);
		}
		// System.out.println(tiesetgraph.TiesetGraph);
		this.TiesetGraph = tiesetgraph.getTiesetGraph();
	}

	private void booleanBorder(){
		for(Node node: globalNode){
			node.ifBorderNode();
		}
	}

	private void set_controller_id(){
		for(int controller_id = 0; controller_id < topologyInfo.size(); controller_id++){
			for(String node_id :topologyInfo.get(controller_id).node_id){
				if(node_id.startsWith("host")){
					String IP =ChangeMac_toIP.get(node_id);
					SearchNode(1).host_controller_map.put(IP, controller_id + 1);
					//System.out.println(SearchNode(1).host_controller_map);
				}
				else{
					String node_id_str = RemoveOpenFlow(node_id);
					int node_id_int = Integer.valueOf(node_id_str);
					SearchNode(node_id_int).controller_id = controller_id + 1;
					SearchNode(node_id_int).controller_id_set .add(controller_id + 1);
					//System.out.println(node_id_int);
					//System.out.println(SearchNode(node_id_int).controller_id);
				}
			}
		}
	}
	
	public void setControllerIP(String ControllerIP1, String ControllerIP2){
		for(Node node : globalNode){
			if(node.controller_id == 1){
				node.belong_to_IP = ControllerIP1;
			}
			if(node.controller_id == 2){
				node.belong_to_IP = ControllerIP2;
			}
		}
	}
	
	public void setControllerIP(String ControllerIP1){
		for(Node node : globalNode){
			if(node.controller_id == 1){
				node.belong_to_IP = ControllerIP1;
			}
		}
	}
	

	private void findBorderEdgeNode(Node[] nodeList){
		for(Node node : nodeList){
			//次ノードが対象ノードのcontroller_id以外である場合
			for(Node next_node : node.mapNextNode.keySet()){
				if(!(node.controller_id == next_node.controller_id)){
					node.CoreNode = true;
					//次ノードのcontroller_idを取得
					node.next_node_controller_id.put(next_node, next_node.controller_id);
				}
			}
		}
	}
}
