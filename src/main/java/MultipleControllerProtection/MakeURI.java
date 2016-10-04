package MultipleControllerProtection;

public class MakeURI {

	public String getTopologyURI(String IP) {
		String URI = "http://"+IP+":8181/restconf/operational/network-topology:network-topology/";
		return URI;
	}

	public String getURI(String IP) {
		String URI = "http://"+IP+":8181/restconf/operations/sal-flow:add-flow";
		return URI;
	}

	public String getGroupURI(String IP) {
		String GroupURI = "http://"+IP+":8181/restconf/operations/sal-group:add-group";
		return GroupURI;
	}

	/*
	 * String PutUri =
	 * "http://localhost:8181/restconf/config/opendaylight-inventory:nodes/node/Node/table/table_id/flow/flow_id";
	 * String PutXml=
	 * "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><flow xmlns=\"urn:opendaylight:flow:inventory\"><strict>false</strict><instructions> <instruction> <order>0</order><apply-actions><action><order>0</order> <dec-nw-ttl/> </action> </apply-actions></instruction></instructions><table_id>1</table_id><id>1</id><cookie_mask>255</cookie_mask> <installHw>false</installHw><match><ethernet-match> <ethernet-type><type>2048</type></ethernet-type></ethernet-match><ipv4-destination>10.0.1.1/24</ipv4-destination></match><hard-timeout>5000</hard-timeout> <cookie>1</cookie><idle-timeout>3400</idle-timeout><flow-name>FooXf1</flow-name> <priority>2</priority> <barrier>false</barrier> </flow> "
	 * ; RestRequest putXML = new RestRequest(); putXML.PutXML(PutUri, PutXml);
	 * 
	 * String PostUri =
	 * "http://localhost:8181/restconf/config/opendaylight-inventory:nodes/node/Node/table/table_id/flow/flow_id";
	 * String PostXml=
	 * "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><flow xmlns=\"urn:opendaylight:flow:inventory\"><strict>false</strict><instructions> <instruction> <order>0</order><apply-actions><action><order>0</order> <dec-nw-ttl/> </action> </apply-actions></instruction></instructions><table_id>1</table_id><id>1</id><cookie_mask>255</cookie_mask> <installHw>false</installHw><match><ethernet-match> <ethernet-type><type>2048</type></ethernet-type></ethernet-match><ipv4-destination>10.0.1.1/24</ipv4-destination></match><hard-timeout>5000</hard-timeout> <cookie>1</cookie><idle-timeout>3400</idle-timeout><flow-name>FooXf1</flow-name> <priority>2</priority> <barrier>false</barrier> </flow> "
	 * ; RestRequest postXML = new RestRequest(); postXML.PutXML(PostUri,
	 * PostXml);
	 */

}
