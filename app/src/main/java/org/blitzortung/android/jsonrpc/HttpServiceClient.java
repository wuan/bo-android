package org.blitzortung.android.jsonrpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpServiceClient {

    private final URL url;
    private final String userAgentString;
    private int socketTimeout = 0;
    private int connectionTimeout = 0;

    HttpServiceClient(String uriString, String agentSuffix) {
        try {
            url = new URI(uriString).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
        userAgentString = "bo-android" + agentSuffix;
    }

    public void shutdown() {
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

    public String doRequest(String data) {
        try {
            return doRequestChecked(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String doRequestChecked(String data) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("POST");

        byte[] postDataBytes = data.getBytes("UTF-8");

        urlConnection.setRequestProperty("Content-Type", "text/json");
        urlConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        urlConnection.setRequestProperty("User-Agent", userAgentString);

        urlConnection.setDoOutput(true);
        urlConnection.getOutputStream().write(postDataBytes);
        urlConnection.setConnectTimeout(getConnectionTimeout());
        urlConnection.setReadTimeout(getSocketTimeout());

        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

        String response = getResponse(reader);

        reader.close();

        return response;
    }

    private String getResponse(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        while (true) {
            final String line = reader.readLine();

            if (line != null) {
                sb.append(line).append('\n');
            } else {
                break;
            }
        }

        return sb.toString().trim();
    }
}
