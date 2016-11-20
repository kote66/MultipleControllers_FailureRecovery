import java.util.List;

@startuml images/OneController.png
class Main{
	-controller_ip:List<String>
-json:String
-getJSON()
-exe(controller_ip, json)
} 
package Flow{

	class MakeFlow{
		#graph:Graph<Node, Edge>
	#tiesetGraph:Graph<Integer,TiesetEdge>
	#controllerGraph:Graph<Controller,Integer>
	#decisionRoute(node, nextNode, dst_IP)
	#judgeDirection(node, nextNode, dst_IP)
	#findOutPort(node, nextNode)
	#findNextController(node, dst_IP)
	#findNextTieset(node, dst_IP)
	#dropAction(dst_ip)
	}

	class EdgeFlow{
		~EdgeFlow(graph, tiesetGraph, controllerGraph)
		-makeXML()
		-makeGroupXML(node, bucketlist)
	}

	class TiesetNodeFlow{
		~TiesetNodeFlow(graph, tiesetGraph)
		-makeXML()
		-makeGroupXML(node, bucketlist)
	}

	class CoreNodeFlow{
		~CoreNodeFlow(graph, tiesetGraph, controllerGraph)
		-makeXML()
		-makeGroupXML(node, bucketlist)
	}

	class BorderNodeFlow{
		~BorderNodeFlow(graph, tiesetGraph, controllerGraph)
		-makeXML()
		-makeGroupXML(node, bucketlist)
	}
}

package Topology{

	class Topology{
		+graph:Graph<Node, Edge>
		+tiesetGraph:Graph<Tieset,Integer>
		+controllerGraph:Graph<Controller, Integer>
		-node_id:List<String>
		-host_ip:List<String>
		-host_mac:List<String>
		-dst_node:List<String>
		-source_node:List<String>
		-dst_tp:String
		-source_tp:String
		-host:String

		Topology(String json)
		-nodeAdd()
		-edgeAdd()
		-nextNodeInfo(dst_tp, source_tp)
		-arrangeEdgeData(dst_node,source_node)
		-arrangeHostData(dst_node,source_node)
		-arrangeNodeData(node)
	}
	
	class Controller{
		
	}
	
	class Node{
		+NodeId:String
		+IntNodeId:int
		+TiesetID:List<Integer>
		+HostID:List<String>
		+Hostmap:Map<String, Integer>
		+TiesetIdtoIP:Map<String, List<Integer>>
		+NodeTypeEdge:String=falth
		+NodeTypeBorder:String=falth
		+NodeTypeBorderEdge:String=falth
		+Node SearchNode(intNodeID)
		+findEdgeNode()
		+findBorderNode()
		+findBorderEdgeNode()
	}

	class Edge{
		+EdgeId:int
		+edgenode:List<Integer>
		+TiesetList:List<Integer>
	}


}

package Tieset{
	class TiesetGraph{
		+tiesetGraph:Graph<Tieset,Integer>

	}
	class TiesetEdge{
		
	}

	class MakeTieset{
		+graph:Graph<Node, Edge>
		+tiesetList:List<List<Integer>>
		-MakeTieset(graph)
	}

	class Tieset{
		+TiesetID:int
		+TiesetList:List<Integer>
		+Tieset(tiesetList, TiesetID)
	}


}


package RestAPI{
	class RestRequest{
		+GetJSON(String uri)
		+PostXML(String uri, String xml)
		+DeleteXML (String uri)
	}

	class MakeXML{
		+POSTURI
		+POSTGroupURI
		+TiesetNodeFlow(node, flowlist)
		+TiesetGroup(node, bucketlist)
		+EdgeFlow(node, flowlist)
		+EdgeGroup(node, bucketlist)
		+BorderNodeFlow(node, flowlist)
		+BorderGroup(node, bucketlist)
		+CoreNodeFlow(node, flowlist)
		+BorderEdgeGroup(node, bucketlist)
		+MakeBucket(bucket_id, watch_port, vlan_id)
		+DropAction(nodeID, IP)
	}

	class RestClient{
		-account:String=admin
				-password:String=admin
				+RestClient(account, password)
				-getClient()
				-sendRequest()
				+put(uri)
				+post(uri)
				+delete(uri)
	}

	class MakeURI{
		+GetTopologyURI()
		+PostFlowURI()
		+PostGroupURI()
	}

}



Main .down.> MakeFlow
EdgeFlow -up-|> MakeFlow
TiesetNodeFlow -up-|> MakeFlow
BorderNodeFlow -up-|> MakeFlow
CoreNodeFlow -up-|> MakeFlow

EdgeFlow "1".down.>"1" Topology
TiesetNodeFlow "1".down.>"1" Topology
BorderNodeFlow "1".down.>"1" Topology
CoreNodeFlow "1".down.>"1" Topology

Topology .down.> TiesetGraph
TiesetEdge "1..*"-down-*"1" TiesetGraph
TiesetGraph "1".down.>"0..*" Tieset
Tieset .down.> MakeTieset

EdgeFlow .down.> MakeXML
TiesetNodeFlow .down.> MakeXML
BorderNodeFlow .down.> MakeXML
CoreNodeFlow .down.> MakeXML
MakeXML .down.> MakeURI

Node "1..*"-down-*"1" Topology
Edge "0..*"-down-*"1" Topology
Controller "1..*"-down-*"1" Topology
MakeTieset "1".down.>"1" Topology

MakeXML .down.> RestRequest
RestRequest .down.> RestClient
@enduml
