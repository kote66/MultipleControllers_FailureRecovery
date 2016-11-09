
package MultipleControllerProtection;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections15.Transformer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.XMLOutputProcessor;
import org.w3c.dom.*;


public class MakeXML {
	
	String PostUri(String IP){
		String PostUri = "http://"+IP+":8181/restconf/operations/sal-flow:add-flow";
		return PostUri;
	}
	
	String PostGroupUri(String IP){
		String PostGroupUri = "http://"+IP+":8181/restconf/operations/sal-group:add-group";
		return PostGroupUri;
	}
	
	RestRequest rest_request = new RestRequest();
	Namespace ns;
	//xmlヘッダーの部分の書き換え
	public static final AbstractXMLOutputProcessor XMLOUTPUT = new AbstractXMLOutputProcessor() {
		@Override
		protected void printDeclaration(final Writer out, final FormatStack fstack) throws IOException {
			write(out, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> ");
			write(out, fstack.getLineSeparator());
		}
	};



	// タイセットノード
	public void tiesetflow(Node node, int tieset_id, int group_id, int priority) throws ParserConfigurationException {
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		vlan_match(match, tieset_id);
		make_instructions(product, group_id);

		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		String test = xout.outputString(document);
		//flow_add
		String IP ;
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
	}


	public void tiesetgroup(Node node, int group_id, List<Integer> watch_port) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);

		Element buckets = make_buckets(product);
		//現用経路のbucket
		Element bucket = make_bucket(buckets);
		int order0 = 0;
		int order1 = 1;
		int order2 = 2;
		int bucket_id0 = 0;
		int bucket_id1 = 1;
		int group_id_second = decisionGroup_id(group_id);
		int F_watch_port = watch_port.get(0);
		int S_watch_port = watch_port.get(1);
		make_bucket(bucket, order0, order1, group_id, F_watch_port);
		output_action(bucket, F_watch_port, bucket_id0, order2);
		
		//迂回経路のbucket
		Element backup_bucket = make_bucket(buckets);
		make_bucket(backup_bucket, order0, order1, group_id_second, S_watch_port);
		backup_output_action(backup_bucket, S_watch_port, bucket_id1, order2);
		
		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		System.out.println(xout.outputString(document));
		
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
	}
	
	// 境界ノード（コントローラ内）
	public String localBorderNodeFlow(Node node, String DstIP, int group_id, int priority) throws ParserConfigurationException {
		//宛先がローカルコントローラ内
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		//DstIPをmatchにする
		make_ethertype_match(match, "2048");
		make_ip_match(match, DstIP+"/32");
		
		//vlan_match(match, group_id);
		make_instructions(product, group_id);
		
		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		
		//flow_add
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return null;
	}

	// 境界ノード（コントローラ外）
	public String BorderNodeFlow(Node node, String mpls, int group_id, int priority) throws ParserConfigurationException {
		//宛先がローカルコントローラ内
		//mplsをmatchにする
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		make_ethertype_match(match, "34887");
		mpls_match(match, mpls);
		make_instructions(product, group_id);

		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		
		//flow_add
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return null;
	}
	
	//ループ障害への対応
	public String BorderNodeFlow_loop(Node node, String DstIP, int in_port_int, int group_id, int priority) throws ParserConfigurationException {
		//宛先がローカルコントローラ内
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		//DstIPをmatchにする
		make_ethertype_match(match, "2048");
		make_ip_match(match, DstIP+"/32");
		Element in_port = new Element("in-port",ns);
		String in_port_str = String.valueOf(in_port_int);
		match.addContent(in_port);
		in_port.addContent(in_port_str);
		
		
		//vlan_match(match, group_id);
		make_instructions(product, group_id);
		
		
		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		
		//flow_add
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return null;
	}
	
	public String BorderNodeGroup(Node node, int group_id, int F_watch_port, int S_watch_port, int F_tieset_id, int S_tieset_id) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);
		//outline_group(product, group_id, node.node_id);
		Element buckets = make_buckets(product);
		//現用経路のbucket
		Element bucket = make_bucket(buckets);
		//vlanの書き換えと出力ポートの指定
		int order0 = 0;
		int order1 = 1;
		int order2 = 2;
		int bucket_id0 = 0;
		int bucket_id1 = 1;
		//int group_id_second = decisionGroup_id(group_id);
		
		make_bucket(bucket, order0, order1, F_tieset_id, F_watch_port);
		output_action(bucket, F_watch_port, bucket_id0, order2);
		//迂回経路のbucket
		Element backup_bucket = make_bucket(buckets);
		make_bucket(backup_bucket, order0, order1, S_tieset_id, S_watch_port);
		output_action(backup_bucket, S_watch_port, bucket_id1, order2);

		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return xout.outputString(document);
	}
	
	//宛先がコントローラ外
	public String EdgeNodeflowPushVlan(Node node, String IP, int group_id, int priority, int in_port_int) throws ParserConfigurationException {
		//宛先がローカルコントローラ内
		//mplsをmatchにする
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		Element in_port = new Element("in-port",ns);
		String in_port_str = String.valueOf(in_port_int);
		make_ethertype_match(match, "2048");
		match.addContent(in_port);
		in_port.addContent(in_port_str);
		make_ip_match(match, IP+"/32");
		make_instructions(product, group_id);

		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		
		//flow_add
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return null;
	}
	
	public String EdgeNodeGroupPushVlan(Node node, int group_id, int F_watch_port, int S_watch_port, int F_tieset_id, int S_tieset_id, int dst_controller) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);
		//outline_group(product, group_id, node.node_id);
		Element buckets = make_buckets(product);
		//現用経路のbucket
		Element bucket = make_bucket(buckets);
		//vlanの書き換えと出力ポートの指定
		int order0 = 0;
		int order1 = 1;
		int order4 = 4;
		int bucket_id0 = 0;
		int bucket_id1 = 1;
		//int group_id_second = decisionGroup_id(group_id);
		
		make_bucket(bucket, order0, order1, F_tieset_id, F_watch_port, dst_controller);
		output_action(bucket, F_watch_port, bucket_id0, order4);
		//迂回経路のbucket
		Element backup_bucket = make_bucket(buckets);
		make_bucket(backup_bucket, order0, order1, S_tieset_id, S_watch_port, dst_controller);
		output_action(backup_bucket, S_watch_port, bucket_id1, order4);

		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return xout.outputString(document);
	}

	public String local_EdgeNodeGroupPushVlan(Node node, int group_id, int F_watch_port, int S_watch_port, int F_tieset_id, int S_tieset_id) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);
		//outline_group(product, group_id, node.node_id);
		Element buckets = make_buckets(product);
		//現用経路のbucket
		Element bucket = make_bucket(buckets);
		//vlanの書き換えと出力ポートの指定
		int order0 = 0;
		int order1 = 1;
		int order2 = 2;
		int bucket_id0 = 0;
		int bucket_id1 = 1;
		//int group_id_second = decisionGroup_id(group_id);
		
		make_bucket(bucket, order0, order1, F_tieset_id, F_watch_port);
		output_action(bucket, F_watch_port, bucket_id0, order2);
		//迂回経路のbucket
		Element backup_bucket = make_bucket(buckets);
		make_bucket(backup_bucket, order0, order1, S_tieset_id, S_watch_port);
		output_action(backup_bucket, S_watch_port, bucket_id1, order2);

		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return xout.outputString(document);
	}

	
	//ホストへ転送
	//Flow
	public String EdgeNodeFlowHost(Node node, String DstIP, int output_port, int priority, int group_id) throws ParserConfigurationException {
		Element product = make_flow_xml(node, priority);
		Element match = make_match(product);
		//DstIPをmatchにする
		make_ethertype_match(match, "2048");
		make_ip_match(match, DstIP+"/32");
		make_instructions(product, group_id);
		
		Document document = new Document(product);
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		
		//flow_add
		rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		//group_add
		
		return null;
	}
	
	//Group
	public String EdgeNodeGroupHost(Node node, String DstIP, int output_port , int group_id) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);
		//outline_group(product, group_id, node.node_id);
		Element buckets = make_buckets(product);

		Element bucket = make_bucket(buckets);
		//vlanの書き換えと出力ポートの指定
		//int group_id_second = decisionGroup_id(group_id);
		make_pop_bucket(bucket, output_port);
		//output_action(bucket, output_port, bucket_id0, order2);

		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
		return xout.outputString(document);
	}

	//コアノード
	public void CoreNodeFlow(Node node, String mpls, int group_id, int priority) throws ParserConfigurationException{
		//mplsをmatchにする
				Element product = make_flow_xml(node, priority);
				Element match = make_match(product);
				make_ethertype_match(match, "34887");
				mpls_match(match, mpls);
				make_instructions(product, group_id);

				Document document = new Document(product);
				XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
				//System.out.println(xout.outputString(document));
				
				//flow_add
				rest_request.PostXML(PostUri(node.belong_to_IP), xout.outputString(document));
				node.flow_counter++;
		
	}
	
	
	public void CoreNodeGroup(Node node, int group_id, int F_watch_port) throws ParserConfigurationException{
		Element product = make_group_xml(group_id, node.node_id);
		//outline_group(product, group_id, node.node_id);
		Element buckets = make_buckets(product);
		//現用経路のbucket
		Element bucket = make_bucket(buckets);
		int order0 = 0;
		int order1 = 1;
		int bucket_id0 = 0;
		int bucket_id1 = 1;
		//make_bucket(bucket, order0, order1, F_tieset_id, F_watch_port, dst_controller);
		output_action(bucket, F_watch_port, bucket_id0, order0);

		Document document = new Document(product);	
		XMLOutputter xout = new XMLOutputter(XMLOUTPUT);
		//System.out.println(xout.outputString(document));
		//flow_group_add
		rest_request.PostXML(PostGroupUri(node.belong_to_IP), xout.outputString(document));
		node.flow_counter++;
	}
	
	
	//XMLの作成（JDOM）
	public Element make_flow_xml(Node node, int priority) throws ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);

		ns = Namespace.getNamespace("urn:opendaylight:flow:service");
		Element product = new Element("input", ns);		
		outlineFlow(product, node.node_id, priority);
		
		return product;
	}

	//Flowの根幹
	private void outlineFlow(Element product, int node_id, int priority_num){
		Element node = new Element("node",ns);
		Namespace inv = Namespace.getNamespace("inv", "urn:opendaylight:inventory");
		node.addNamespaceDeclaration(inv);
		product.addContent(node);
		node.addContent("/inv:nodes/inv:node[inv:id=\"openflow:"+node_id+"\"]");

		Element priority = new Element("priority",ns);
		Element table_id = new Element("table_id",ns);

		product.addContent(priority);
		String priority_str = String.valueOf(priority_num);
		priority.addContent(priority_str);

		product.addContent(table_id);
		table_id.addContent("0");
	}

	public Element make_match(Element product){
		Element match = new Element("match",ns);
		product.addContent(match);
		return match;
	}

	public void make_ethertype_match(Element match, String ethertype){
		Element ethernet_match = new Element("ethernet-match",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		match.addContent(ethernet_match);
		ethernet_match.addContent(ethernet_type);
		ethernet_type.addContent(new Element("type",ns).setText(ethertype));
	}

	public void vlan_match(Element match, int tieset_id){
		Element vlan_match = new Element("vlan-match",ns);
		Element vlan_id = new Element("vlan-id",ns);
		Element vlan_id_inter = new Element("vlan-id",ns);
		Element vlan_id_present = new Element("vlan-id-present",ns);
		String vlan_id_str = String.valueOf(tieset_id);

		vlan_id_present.addContent("true");
		match.addContent(vlan_match);
		vlan_match.addContent(vlan_id);
		vlan_id.addContent(vlan_id_present);
		vlan_id.addContent(vlan_id_inter);
		vlan_id_inter.addContent(vlan_id_str);
		//vlan_id_inter.addContent(new Element("vlan-id",ns).setText(vlan_id_str));
		make_ethertype_match(match, "2048");
	}

	private void mpls_match(Element match, String mpls){
		Element protocol_match_fields = new Element("protocol-match-fields",ns);
		match.addContent(protocol_match_fields);
		protocol_match_fields.addContent(new Element("mpls-label",ns).setText(mpls));
	}

	public void make_ip_match(Element match, String IP){
		match.addContent(new Element("ipv4-destination",ns).setText(IP));
	}

	public void inport_match(Element match, String inport){
		match.addContent(new Element("in-port",ns).setText(inport));
	}

	public void make_order(Element action, String order){
		action.addContent(new Element("order",ns).setText(order));
	}

	private void make_instructions(Element product, int group_id_int){
		Element instructions = new Element("instructions",ns);
		Element instruction = new Element("instruction",ns);
		Element apply_actions = new Element("apply-actions",ns);
		Element action = new Element("action",ns);
		Element group_action = new Element("group-action",ns);
		String group_id_str = String.valueOf(group_id_int);

		product.addContent(instructions);
		instructions.addContent(instruction);
		instruction.addContent(new Element("order",ns).setText("0"));
		instruction.addContent(apply_actions);
		apply_actions.addContent(action);
		action.addContent(group_action);
		action.addContent(new Element("order",ns).setText("0"));
		group_action.addContent(new Element("group-id",ns).setText(group_id_str));
	}
	
	private void make_pop_bucket(Element bucket, int output_port_int){
		Element action_pop_vlan = new Element("action",ns);
		Element action_pop_mpls = new Element("action",ns);
		Element action_output_port = new Element("action",ns);
		Element pop_mpls_action = new Element("pop-mpls-action",ns);
		Element pop_vlan_action = new Element("pop-vlan-action",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		Element output_action = new Element("output-action",ns);
		Element watch_port = new Element("watch_port",ns);
		Element bucket_id = new Element("bucket-id",ns);
		
		Element output_node_connector = new Element("output-node-connector",ns);
		String output_port = String.valueOf(output_port_int);
		
		bucket.addContent(action_pop_vlan);
		action_pop_vlan.addContent(pop_vlan_action);
		action_pop_vlan.addContent(new Element("order",ns).setText("0"));
		
		bucket.addContent(action_pop_mpls);
		action_pop_mpls.addContent(new Element("order",ns).setText("1"));
		action_pop_mpls.addContent(pop_mpls_action);
		pop_mpls_action.addContent(ethernet_type);
		ethernet_type.addContent("2048");
		
		bucket.addContent(action_output_port);
		action_output_port.addContent(new Element("order",ns).setText("2"));
		action_output_port.addContent(output_action);
		output_action.addContent(output_node_connector);
		output_node_connector.addContent(output_port);
		
		bucket.addContent(watch_port);
		watch_port.addContent(output_port);
		bucket.addContent(bucket_id);
		bucket_id.addContent("0");
	}
	
	private void make_edgenode_instructions(Element product, int output_port_int){
		Element instructions = new Element("instructions",ns);
		Element instruction = new Element("instruction",ns);
		Element apply_actions = new Element("apply-actions",ns);
		Element action_pop_vlan = new Element("action",ns);
		Element action_pop_mpls = new Element("action",ns);
		Element action_output_port = new Element("action",ns);
		Element pop_vlan_action = new Element("pop-vlan-action",ns);
		Element pop_mpls_action = new Element("pop-mpls-action",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		Element output_action = new Element("output-action",ns);
		Element output_node_connector = new Element("output-node-connector",ns);
		String output_port = String.valueOf(output_port_int);
		
		product.addContent(instructions);
		instructions.addContent(instruction);
		instruction.addContent(new Element("order",ns).setText("0"));
		instruction.addContent(apply_actions);
		
		apply_actions.addContent(action_pop_vlan);
		apply_actions.addContent(action_pop_mpls);
		apply_actions.addContent(action_output_port);
		
		action_pop_mpls.addContent(pop_mpls_action);
		pop_mpls_action.addContent(ethernet_type);
		ethernet_type.addContent("2048");
		action_output_port.addContent(output_action);
		output_action.addContent(output_node_connector);
		output_node_connector.addContent(output_port);
		
		action_pop_vlan.addContent(new Element("order",ns).setText("0"));
		action_pop_vlan.addContent(pop_vlan_action);
		action_pop_mpls.addContent(new Element("order",ns).setText("1"));
		action_output_port.addContent(new Element("order",ns).setText("2"));
	}

	
	//in_portをmatch条件にするXML

	//IPアドレスをmatch条件にするXML


	public Element make_group_xml(int group_id, int node_id) throws ParserConfigurationException{
		//DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);

		ns = Namespace.getNamespace("urn:opendaylight:group:service");
		Element product = new Element("input", ns);		
		outline_group(product, group_id, node_id);
		return product;
	}

	private void outline_group(Element product, int group_id_int, int node_id_int){
		Element node = new Element("node",ns);
		Element group_id = new Element("group-id",ns);
		Element group_type = new Element("group-type",ns);
		String group_id_str = String.valueOf(group_id_int);
		String node_id = String.valueOf(node_id_int);
		
		Namespace inv = Namespace.getNamespace("inv", "urn:opendaylight:inventory");
		node.addNamespaceDeclaration(inv);
		product.addContent(node);
		product.addContent(group_type);
		group_type.addContent("group-ff");
		node.addContent("/inv:nodes/inv:node[inv:id=\"openflow:"+node_id+"\"]");

		product.addContent(group_id);
		group_id.addContent(group_id_str);
	}

	//buckets
	private Element make_buckets(Element product){
		Element buckets = new Element("buckets",ns);
		product.addContent(buckets);
		return buckets;
	}

	private Element make_bucket(Element buckets){
		Element bucket = new Element("bucket",ns);
		buckets.addContent(bucket);
		return bucket;
	}

	private void make_action_group(Element bucket, String order){
		Element action = new Element("action",ns);
		action.addContent(order);
	}

	private void make_bucket(Element bucket, int order1_int, int order2_int, int tieset_id, int watch_port){
		Element action1 = new Element("action",ns);
		Element action2 = new Element("action",ns);
		Element push_vlan_action = new Element("push-vlan-action",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		Element order1 = new Element("order",ns);
		Element order2 = new Element("order",ns);
		Element set_field = new Element("set-field",ns);
		String order1_str = String.valueOf(order1_int);
		String order2_str = String.valueOf(order2_int);
		
		bucket.addContent(action1);
		//ether_typeの設定
		action1.addContent(push_vlan_action);
		push_vlan_action.addContent(ethernet_type);
		ethernet_type.addContent("33024");
		//オーダー
		order1.addContent(order1_str);
		action1.addContent(order1);
		
		//2つめのaction
		bucket.addContent(action2);
		order2.addContent(order2_str);
		action2.addContent(order2);
		action2.addContent(set_field);

		Element vlan_match = new Element("vlan-match",ns);
		Element vlan_id = new Element("vlan-id",ns);
		Element vlan_id_inter = new Element("vlan-id",ns);
		Element vlan_id_present = new Element("vlan-id-present",ns);
		String vlan_id_str = String.valueOf(tieset_id);

		set_field.addContent(vlan_match);
		vlan_match.addContent(vlan_id);
		vlan_id.addContent(vlan_id_present);
		vlan_id_present.addContent("true");
		vlan_id.addContent(vlan_id_inter);
		vlan_id_inter.addContent(vlan_id_str);
		//vlan_id_inter.addContent(new Element("vlan-id",ns).setText(vlan_id_str));
		
		//3つめのAction
		//output
	}


	private void make_bucket(Element bucket, int order1_int, int order2_int, int tieset_id, int watch_port, int dst_controller_id){
		Element action1 = new Element("action",ns);
		Element action2 = new Element("action",ns);
		Element push_vlan_action = new Element("push-vlan-action",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		Element order1 = new Element("order",ns);
		Element order2 = new Element("order",ns);
		Element set_field = new Element("set-field",ns);
		String order1_str = String.valueOf(order1_int);
		String order2_str = String.valueOf(order2_int);
		
		bucket.addContent(action1);
		//ether_typeの設定
		action1.addContent(push_vlan_action);
		push_vlan_action.addContent(ethernet_type);
		ethernet_type.addContent("33024");
		//オーダー
		order1.addContent(order1_str);
		action1.addContent(order1);
		
		//2つめのaction
		bucket.addContent(action2);
		order2.addContent(order2_str);
		action2.addContent(order2);
		action2.addContent(set_field);

		Element vlan_match = new Element("vlan-match",ns);
		Element vlan_id = new Element("vlan-id",ns);
		Element vlan_id_inter = new Element("vlan-id",ns);
		Element vlan_id_present = new Element("vlan-id-present",ns);
		String vlan_id_str = String.valueOf(tieset_id);

		set_field.addContent(vlan_match);
		vlan_match.addContent(vlan_id);
		vlan_id.addContent(vlan_id_present);
		vlan_id_present.addContent("true");
		vlan_id.addContent(vlan_id_inter);
		vlan_id_inter.addContent(vlan_id_str);
		//vlan_id_inter.addContent(new Element("vlan-id",ns).setText(vlan_id_str));
		
		//mplsのAction
		String mpls_label = String.valueOf(dst_controller_id);
		mpls_action(bucket, mpls_label);
	}
	
	private void mpls_action(Element bucket, String mpls){
		Element action1 = new Element("action",ns);
		Element action2 = new Element("action",ns);
		Element push_mpls_action = new Element("push-mpls-action",ns);
		Element ethernet_type = new Element("ethernet-type",ns);
		Element set_field = new Element("set-field",ns);
		Element protocol_match_fields = new Element("protocol-match-fields",ns);
		Element mpls_label = new Element("mpls-label",ns);		
		Element order1 = new Element("order",ns);
		Element order2 = new Element("order",ns);

		bucket.addContent(action1);
		action1.addContent(order1);
		order1.addContent("2");
		action1.addContent(push_mpls_action);
		push_mpls_action.addContent(ethernet_type);
		ethernet_type.addContent("34887");

		bucket.addContent(action2);
		action2.addContent(order2);
		order2.addContent("3");
		action2.addContent(set_field);
		set_field.addContent(protocol_match_fields);
		protocol_match_fields.addContent(mpls_label);
		mpls_label.addContent(mpls);
	}

	private void output_action(Element bucket, int output_port_int, int bucket_id_int, int order_int){
		Element output_action = new Element("output-action",ns);
		Element output_node_connector = new Element("output-node-connector",ns);
		Element action = new Element("action",ns);
		Element order = new Element("order",ns);
		Element bucket_id = new Element("bucket-id",ns);
		Element watch_port = new Element("watch_port",ns);
		String output_port_str = String.valueOf(output_port_int);
		String bucket_id_str = String.valueOf(bucket_id_int);
		String order_str = String.valueOf(order_int);
		
		bucket.addContent(action);
		action.addContent(output_action);
		output_action.addContent(output_node_connector);
		output_node_connector.addContent(output_port_str);
		action.addContent(order);
		order.addContent(order_str);
		
		bucket.addContent(bucket_id);
		bucket_id.addContent(bucket_id_str);
		bucket.addContent(watch_port);
		watch_port.addContent(output_port_str);
	}
	
	private void backup_output_action(Element bucket, int output_port_int, int bucket_id_int, int order_int){
		Element output_action = new Element("output-action",ns);
		Element output_node_connector = new Element("output-node-connector",ns);
		Element action = new Element("action",ns);
		Element order = new Element("order",ns);
		Element bucket_id = new Element("bucket-id",ns);
		Element watch_port = new Element("watch_port",ns);
		String output_port_str = String.valueOf(output_port_int);
		String bucket_id_str = String.valueOf(bucket_id_int);
		String order_str = String.valueOf(order_int);
		
		bucket.addContent(action);
		action.addContent(output_action);
		output_action.addContent(output_node_connector);
		output_node_connector.addContent("INPORT");
		action.addContent(order);
		order.addContent(order_str);
		
		bucket.addContent(bucket_id);
		bucket_id.addContent(bucket_id_str);
		bucket.addContent(watch_port);
		watch_port.addContent(output_port_str);
	}
	
	private int decisionGroup_id(int group_id){
		int return_group_id=0;
		if(group_id < 2048){
			return_group_id = 2048 + group_id;
		}
		if(2048 < group_id){
			return_group_id = -2048 + group_id;
		}
		
		return return_group_id;
	}
	
	// Dropするためのフロー
	public String DropAction(int NodeID, String IP) {
		String DropAction = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><input xmlns=\"urn:opendaylight:flow:service\"><barrier>false</barrier><node xmlns:inv=\"urn:opendaylight:inventory\">/inv:nodes/inv:node[inv:id=\"openflow:"
				+ NodeID
				+ "\"]</node><cookie>1</cookie><installHw>false</installHw><match><ethernet-match><ethernet-type><type>2048</type></ethernet-type></ethernet-match><ipv4-destination>"
				+ IP
				+ "/32</ipv4-destination></match><instructions><instruction><order>0</order><apply-actions><action><order>0</order><drop-action/></action></apply-actions></instruction></instructions><priority>50</priority><strict>false</strict><table_id>0</table_id></input>";
		return DropAction;
	}
}
