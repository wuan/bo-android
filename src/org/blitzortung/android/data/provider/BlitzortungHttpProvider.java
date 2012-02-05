package org.blitzortung.android.data.provider;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

public class BlitzortungHttpProvider extends DataProvider {

	static final String TAG = "BlitzortungHttpProvider";

	@Override
	public DataResult<Stroke> getStrokes(int timeInterval) {

		DataResult<Stroke> dataResult = new DataResult<Stroke>();

		HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {
			public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
				AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

				if (authState.getAuthScheme() == null) {
					AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
					Credentials creds = credsProvider.getCredentials(authScope);
					if (creds != null) {
						authState.setAuthScheme(new BasicScheme());
						authState.setCredentials(creds);
					}
				}
			}
		};

		DefaultHttpClient client = new DefaultHttpClient();
		client.addRequestInterceptor(preemptiveAuth, 0);

		return dataResult;
	}

	@Override
	public DataResult<Station> getStations() {
		return new DataResult<Station>();
	}

	@Override
	public ProviderType getType() {
		return ProviderType.HTTP;
	}

}
