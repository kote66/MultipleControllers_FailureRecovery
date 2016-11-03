package MultipleControllerProtection;



public class DropAction extends MakeFlow{
	
	String PostUri = "http://localhost:8181/restconf/operations/sal-flow:add-flow";
	DropAction(Node[] globalNode){
		this.globalNode = globalNode;
	}
	
	// 各スイッチにdropを設定する
		public void dropAction() {
			for (Node node : globalNode) {
				for (String IP : node.IPSet) {
					// System.out.println("Node"+node.IntNodeId+"へ"+"宛先"+IP+"への通信をdropするフロー追加");
					String dropaction = makexml.DropAction(node.node_id, IP);
					//postXML.PostXML(PostUri, dropaction);
				}
			}
		}
}
