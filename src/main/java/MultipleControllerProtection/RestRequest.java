package MultipleControllerProtection;

import javax.ws.rs.core.MediaType;

public class RestRequest {
	public String GetJSON(String uri) {
		RestClient client = new RestClient("admin", "admin");
		String json = client.getString(uri, MediaType.APPLICATION_JSON_TYPE);
		return json;
	}

	public void PutXML(String uri, String xml) {
		RestClient client = new RestClient("admin", "admin");
		client.put(uri, xml.toString(), String.class, MediaType.APPLICATION_XML_TYPE);
	}

	public void PostXML(String uri, String xml) {
		RestClient client = new RestClient("admin", "admin");
		client.post(uri, xml.toString(), String.class, MediaType.APPLICATION_XML_TYPE);
	}

	public void Delete(String uri) {
		RestClient client = new RestClient("admin", "admin");
		client.delete(uri);
	}
}