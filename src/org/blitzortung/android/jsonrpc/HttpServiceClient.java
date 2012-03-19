package org.blitzortung.android.jsonrpc;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class HttpServiceClient {

	private int socketTimeout = 0;
	
	private int connectionTimeout = 0;
	
	private final static ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	private String serviceUri;
	
	private HttpClient httpClient;
	
	HttpServiceClient(String uri) {
        httpClient = new DefaultHttpClient();
        serviceUri = uri;
	}
	
	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	protected String doRequest(AbstractHttpEntity data) {
        // Create HTTP/POST request with a JSON entity containing the request
        HttpPost request = new HttpPost(serviceUri);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, getConnectionTimeout());
        HttpConnectionParams.setSoTimeout(params, getSocketTimeout());
        HttpProtocolParams.setVersion(params, PROTOCOL_VERSION);
        request.setParams(params);
        request.setEntity(data);
        
        String responseString = "";
        
		try {
	        long startTime = System.currentTimeMillis();
	        HttpResponse response = httpClient.execute(request);
	        responseString = EntityUtils.toString(response.getEntity());
	        Log.d("jsonrpc", String.format("request time %d ms (%d bytes received)", System.currentTimeMillis() - startTime, responseString.length()));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return responseString.trim();
	}	
}
