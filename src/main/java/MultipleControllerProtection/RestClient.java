package MultipleControllerProtection;

import javax.ws.rs.HttpMethod;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * RESTリクエストを行うためのクライアントクラス
 * 
 * @author masao.suda
 */
public class RestClient {

	private String account = null;
	private String password = null;

	public RestClient(String account, String password) {
		this.account = account;
		this.password = password;
	}

	private Client getClient() {
		Client client = new Client();
		client.addFilter(new HTTPBasicAuthFilter(account, password));
		return client;
	}

	public String getString(String url, MediaType type) {
		Client client = getClient();
		WebResource resource = client.resource(url);
		ClientResponse response = resource.accept(type).get(ClientResponse.class);
		switch (response.getStatus()) {
		case 200: // OK
			break;
		default:
			return String.format("Code:%s Entity:%s", response.getStatus(), response.getEntity(String.class));
		}
		return response.getEntity(String.class);
	}

	// RESTful Webサービスに対してPOST or PUTメソッドを送信する.
	private <E> String sendRequest(String uri, E entity, String method, Class<E> cls, MediaType type) {

		Client client = getClient();
		ClientRequest.Builder builder = ClientRequest.create();
		try {
			builder.type(type).entity(entity);
			ClientRequest request = builder.build(new URI(uri), method);
			ClientResponse response = client.handle(request);
			switch (response.getStatus()) {
			case 200: // OK
			case 201: // CREATED
				return response.getEntity(String.class);
			default: // OK, CREATED 以外
				String error = response.getEntity(String.class);
				throw new RuntimeException(error);
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e.getMessage());
		}

	}

	// RESTful Webサービスに対してPUTメソッドを送信する.
	public <E> String put(String uri, E entity, Class<E> cls, MediaType type) {
		return sendRequest(uri, entity, HttpMethod.PUT, cls, type);
	}

	// RESTful Webサービスに対してPOSTメソッドを送信する.
	public <E> String post(String uri, E entity, Class<E> cls, MediaType type) {
		return sendRequest(uri, entity, HttpMethod.POST, cls, type);
	}

	// RESTful Webサービスに対してDELETEメソッドを送信する.
	public void delete(String uri) {
		Client client = getClient();
		ClientRequest.Builder builder = ClientRequest.create();
		try {
			ClientRequest request = builder.build(new URI(uri), HttpMethod.DELETE);
			ClientResponse response = client.handle(request);
			if (response.getStatus() != 204) { // NO CONTENT以外
				String error = response.getEntity(String.class);
				throw new RuntimeException(error);
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
