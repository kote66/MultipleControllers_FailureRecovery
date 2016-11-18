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
	int controller_num;
	Node[] globalNode;
	List<Tieset> tiesetList = new ArrayList<Tieset>();
	Graph<Node, Integer> globalGraph = new UndirectedSparseGraph<Node, Integer>();
	Graph<Integer, TiesetEdge> TiesetGraph = new UndirectedSparseGraph<Integer, TiesetEdge>();
	ArrayList<Integer> NodeNextNode = new ArrayList<Integer>();
	Map<String, String> ChangeMac_toIP = new HashMap<String, String>();
	List<TopologyInfo> local_topology = new ArrayList<TopologyInfo>();
	List<Integer> globalNode_ID = new ArrayList<Integer>();	
	List<String> global_src_tp = new ArrayList<String>();
	List<String> global_dst_tp = new ArrayList<String>();
	List<String> globalEdge = new ArrayList<String>();
	List<String> hostInfoList = new ArrayList<String>();
	String host = "host";

	Topology(List<String> jsonlist) {
		controller_num = jsonlist.size();
		setController(controller_num);
		
		//JSON情報をオブジェクトとして扱えるように処理
		for(int i = 0; i < controller_num; i++){
			
			ObjectMapper mapper = new ObjectMapper();
			try {
				local_topology.get(i).rootNode = (mapper.readTree(jsonlist.get(i)));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	
	public void exeTopology() {
		//コントローラ毎のトポロジ情報を整理
		for(int num = 0; num < local_topology.size(); num ++){
			Arrange(local_topology.get(num).rootNode, num);
			addNode(num);
		}
		//重複しているリンクの除去
		removeRepetition();
		
		//全体グラフの作成
		addGrobalNode();	
		addGrobalEdge(global_src_tp, global_dst_tp);
		setHostInfo();
		
		//タイセットの作成
		MakeTieset maketieset = new MakeTieset(globalGraph, globalNode);
		this.tiesetList =maketieset.tiesetList;
		addTiesetIDtoNode();
		
		//タイセットグラフの作成
		MakeTiesetGraph();
		
		//境界ノードの判定
		booleanBorder();
		


		//コントローラ毎にタイセットを作成
		makeTiesetEachController();
		
		//全体グラフとローカルグラフにコントローラIDを設定
		set_controller_id();
		setNextControllerID(globalNode);
	}



	private void Arrange(JsonNode rootNode, int controller_id) {
		//各コントローラにトポロジ情報追加
		//リファクタリング：local_topologyクラスで処理する
		local_topology.get(controller_id).node_id = rootNode.findValuesAsText("node-id");
		local_topology.get(controller_id).node_id.remove("controller-config"); // ノード情報に不必要な情報を取り除く
		local_topology.get(controller_id).host_ip = rootNode.findValuesAsText("ip");
		local_topology.get(controller_id).host_mac = rootNode.findValuesAsText("mac");
		local_topology.get(controller_id).dst_node = rootNode.findValuesAsText("dest-node");
		local_topology.get(controller_id).source_node = rootNode.findValuesAsText("source-node");
		local_topology.get(controller_id).dst_tp = rootNode.findValuesAsText("dest-tp");
		local_topology.get(controller_id).source_tp = rootNode.findValuesAsText("source-tp");

		//全体のトポロジ情報の作成
		for(String dst_tp:rootNode.findValuesAsText("dest-tp")){
			global_dst_tp.add(dst_tp);
		}
		for(String src_tp:rootNode.findValuesAsText("source-tp")){
			global_src_tp.add(src_tp);
		}

		//コントローラ毎のトポロジ情報の作成
		for(String dst_tp:rootNode.findValuesAsText("dest-tp")){
			local_topology.get(controller_id).dst_tp.add(dst_tp);
		}
		for(String src_tp:rootNode.findValuesAsText("source-tp")){
			local_topology.get(controller_id).source_tp.add(src_tp);
		}

		//MACアドレスとIPアドレスの紐付け
		for(int i=0; i < local_topology.get(controller_id).host_mac.size();i++){
			String host_mac = local_topology.get(controller_id).host_mac.get(i);
			String host_ip = local_topology.get(controller_id).host_ip.get(i);
			ChangeMac_toIP.put("host:" + host_mac, host_ip );
		}
	}

	private void removeRepetition(){
		//不必要な情報を削除
		removeHost(global_src_tp, global_dst_tp);
		//重複しているエッジを削除
		removeEdgeRepetition(global_src_tp, global_dst_tp);
	}

	private void removeRepetition_local(int num){
		//不必要な情報を削除
		removeHost(local_topology.get(num).source_tp, local_topology.get(num).dst_tp);
		//コントローラ内の重複しているエッジを削除
		findEdgeRepetition_local(local_topology.get(num).source_tp, local_topology.get(num).dst_tp, num);
	}

	private void addGrobalNode() {
		globalNode = new Node[globalNode_ID.size()];
		for (int i = 0; i < globalNode_ID.size(); i++) {
			globalNode[i] = new Node(globalNode_ID.get(i));
			globalGraph.addVertex(globalNode[i]);
		}
	}

	private void addNode2(int num) {
		local_topology.get(num).node = new Node[local_topology.get(num).node_id_int.size()];
		for (int i = 0; i < local_topology.get(num).node_id_int.size(); i++) {
			local_topology.get(num).node[i] = new Node(local_topology.get(num).node_id_int.get(i));
		}
	}


	private void addNode(int num){
		for (String mynode : local_topology.get(num).node_id) {
			if (!mynode.startsWith(host)) {
				String mynode2 = RemoveOpenFlow(mynode);
				Integer NodeId = Integer.parseInt(mynode2);
				local_topology.get(num).node_id_int.add(NodeId);
				globalNode_ID.add(NodeId);
			}
		}
	}

	private void addGrobalEdge(List<String> src_tp, List<String> dst_tp){
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
			globalGraph.addEdge(i, SearchNode(src_node),SearchNode(dst_node));
		}
	}

	private void addEdge_local(List<String> src_tp, List<String> dst_tp,int num){
		for(int i = 0; i < src_tp.size(); i++){
			String[] src_Info = src_tp.get(i).split(":");
			String[] dst_Info = dst_tp.get(i).split(":");
			int src_node =Integer.parseInt(src_Info[1]);
			int dst_node =Integer.parseInt(dst_Info[1]);
			int src_node_tp =Integer.parseInt(src_Info[2]);
			int dst_node_tp =Integer.parseInt(dst_Info[2]);
			SearchNode_part(src_node,num).neighborNode.add(SearchNode_part(dst_node,num));
			SearchNode_part(dst_node,num).neighborNode.add(SearchNode_part(src_node,num));
			SearchNode_part(src_node,num).mapNextNode.put(SearchNode_part(dst_node,num), src_node_tp);
			SearchNode_part(dst_node,num).mapNextNode.put(SearchNode_part(src_node,num), dst_node_tp);
			//ここで不必要なエッジを取り除くのもあり
			local_topology.get(num).graph.addEdge(i, SearchNode_part(src_node,num),SearchNode_part(dst_node,num));
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
	
	private Node SearchNode_part(int node_id, int num) {
		for (Node nodenum : local_topology.get(num).node) {
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
			Node.host_node_map.put(IP, SearchNode(node));
			//Edgenode判定
			SearchNode(node).Edgenode = true;
			SearchNode(node);
			//ホストIPをリストで保持
			Node.IPSet.add(IP);
		}
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

	//コントローラ内でのエッジ重複をなくす
	private void findEdgeRepetition_local(List<String> src_tp, List<String> dst_tp, int num){
		for(int i = 0; i < src_tp.size(); i++){
			for(int j = 0; j < src_tp.size(); j++){
				if(src_tp.get(i).equals(local_topology.get(num).dst_tp.get(j))&& src_tp.get(i).equals(local_topology.get(num).dst_tp.get(j))){
					local_topology.get(num).source_tp.remove(j);
					local_topology.get(num).dst_tp.remove(j);
					j--;
				}
			}
		}
	}

	private void removeEdgeRepetition_part(){
		for(int i = 0; i < local_topology.get(0).source_tp.size(); i++){
			for(int j = 0; j < local_topology.get(1).source_tp.size(); j++){
				//違うコントローラとエッジを重複させている部分を見つける
				if((local_topology.get(0).source_tp.get(i).equals(local_topology.get(1).dst_tp.get(j)) && (local_topology.get(0).dst_tp.get(i).equals(local_topology.get(1).source_tp.get(j))))){
					local_topology.get(0).source_tp.remove(i);
					local_topology.get(0).dst_tp.remove(i);
					local_topology.get(1).source_tp.remove(j);
					local_topology.get(1).dst_tp.remove(j);
					i--;
				}
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
			for(Node node:tiesetList.get(i).nodeList){
				node.TiesetID.add(i);
			}
		}
	}

	//ノードにタイセットIDをリストで追加。コントローラ内
	private void addTiesetIDtoNode_part(int num){
		//タイセットIDが保存されている
		//System.out.println(local_topology.get(num).tiesetList.size());
		for(int i = 0; i < local_topology.get(num).tiesetList.size(); i++){
			for(Node node:local_topology.get(num).tiesetList.get(i).nodeList){
				node.TiesetID.add(i);
				//System.out.println("test"+i);
			}
		}
	}

	//タイセットグラフの作成
	private void MakeTiesetGraph() {
		TiesetGraph tiesetgraph = new TiesetGraph();
		for (int i = 0; i < globalNode.length - 2; i++) {
			tiesetgraph.MakeTiesetGraph(globalNode[i].TiesetID);
		}
		this.TiesetGraph = tiesetgraph.getTiesetGraph();
	}

	//コントローラ毎にタイセットグラフを作成
	private void MakeTiesetGraph_part(int num) {
		TiesetGraph tiesetgraph = new TiesetGraph();
		for (int i = 0; i < local_topology.get(num).node.length - 2; i++) {
			tiesetgraph.MakeTiesetGraph(local_topology.get(num).node[i].TiesetID);
			//System.out.println(local_topology.get(num).node[i].TiesetID);
		}
		local_topology.get(num).TiesetGraph = tiesetgraph.getTiesetGraph();
	}

	private void set_controller_id(){
		for(int controller_id = 0; controller_id < local_topology.size(); controller_id++){
			for(String node_id :local_topology.get(controller_id).node_id){
				//System.out.println("test"+node_id + "controller_id"+controller_id);
				if(node_id.startsWith("host")){
					
				}
				else{
					String node_id_str = RemoveOpenFlow(node_id);
					int node_id_int = Integer.valueOf(node_id_str);
					SearchNode(node_id_int).controller_id = controller_id + 1;
					Node.controller_id_set.add(controller_id + 1);
					
					//ローカルコントローラへの設定
					//コントローラが2つ以上ある場合
					if(1 < local_topology.size()){
						SearchNode_part(node_id_int,controller_id).controller_id = controller_id + 1;						
					}
				}
			}
		}
	}
	/*
	private void local_set_controller_id(){
		for(int controller_id = 0; controller_id < local_topology.size(); controller_id++){
			for(String node_id :local_topology.get(controller_id).node_id){
				//System.out.println("test"+node_id + "controller_id"+controller_id);
				if(node_id.startsWith("host")){
					
				}
				else{
					String node_id_str = RemoveOpenFlow(node_id);
					int node_id_int = Integer.valueOf(node_id_str);
					SearchNode(node_id_int).controller_id = controller_id + 1;
					Node.controller_id_set.add(controller_id + 1);
					
					//ローカルコントローラへの設定
					//SearchNode_part(node_id_int,controller_id).controller_id = controller_id + 1;
					//SearchNode_part(node_id_int,controller_id).controller_id_set.add(controller_id + 1);
					//System.out.println("重複有無テスト"+node_id_int+"controller_id"+SearchNode(node_id_int).controller_id);
					//System.out.println(SearchNode(node_id_int).controller_id);
				}
			}
		}
	}
	*/

	//リファクタリング：コントローラが3つ以上あっても対応可能にする
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


	private void setNextControllerID(Node[] nodeList){
		for(Node node : nodeList){
			//次ノードが対象ノードのcontroller_id以外である場合
			for(Node next_node : node.mapNextNode.keySet()){
				if(!(node.controller_id == next_node.controller_id)){
					node.CoreNode = true;
					//次ノードのcontroller_idを取得
					Node.next_node_controller_id.put(next_node, next_node.controller_id);
				}
			}
		}
	}
	
	private void booleanBorder(){
		for(Node node: globalNode){
			node.ifBorderNode();
		}
	}
	
	private void makeTiesetEachController(){
		if(1 < controller_num){
			MakeTieset[] maketieset_part = new MakeTieset[local_topology.size()];
			//重複したリンクの除去
			removeEdgeRepetition_part();
			for(int num = 0; num < local_topology.size(); num++){
				//不必要な情報を除去（JSON情報に不必要な情報が混ざるため）
				removeRepetition_local(num);
				
				//グラフの作成
				addNode2(num);
				addEdge_local(local_topology.get(num).source_tp, local_topology.get(num).dst_tp, num);
				maketieset_part[num] = new MakeTieset(local_topology.get(num).graph, local_topology.get(num).node);
				local_topology.get(num).tiesetList =maketieset_part[num].tiesetList;
				addTiesetIDtoNode_part(num);
				MakeTiesetGraph_part(num);
			}
		}
	}
	
	private void setController(int controller_num){
		//コントローラの数に応じてcontrolleを変える
		if(controller_num == 1){
			TopologyInfo controller1 = new TopologyInfo();
			local_topology.add(controller1);
		}
		
		if(controller_num == 2){
			TopologyInfo controller1 = new TopologyInfo();
			TopologyInfo controller2 = new TopologyInfo();
			local_topology.add(controller1);
			local_topology.add(controller2);
		}
		
		if(controller_num == 3){
			TopologyInfo controller1 = new TopologyInfo();
			TopologyInfo controller2 = new TopologyInfo();
			TopologyInfo controller3 = new TopologyInfo();
			local_topology.add(controller1);
			local_topology.add(controller2);
			local_topology.add(controller3);
		}
		
	}
	
	public void showTest(){
		System.out.println("全体グラフ");
		System.out.println(globalGraph.toString());
		System.out.println("全体グラフ");
		System.out.println(TiesetGraph);
		System.out.println("test");
		System.out.println(globalNode[3].controller_id);
		System.out.println("----------------------------------------------------------");
		for(int num = 0; num < local_topology.size(); num++){
			System.out.println("コントローラ"+num+"のグラフ");
			System.out.println(local_topology.get(num).graph);
			System.out.println("----------------------------------------------------------");
			System.out.println("コントローラ"+num+"のグラフ");
			System.out.println(local_topology.get(num).TiesetGraph);
			System.out.println("----------------------------------------------------------");
		}
	}
}
