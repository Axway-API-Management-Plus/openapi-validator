package com.axway.apim.openapi.validator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

public class APIManagerHttpClient {

	private static final String API_VERSION = "/api/portal/v1.4";

	private final URI apiManagerUrl;
	private final String password;
	private final String username;

	private CloseableHttpClient httpClient = null;

	private HttpClientContext clientContext;

	public APIManagerHttpClient(String apiManagerUrl, String username, String password) throws Exception {
		super();
		this.apiManagerUrl = new URI(apiManagerUrl);
		this.password = password;
		this.username = username;
		getClient();
	}

	public void getClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put( new HttpHost(apiManagerUrl.getHost(), apiManagerUrl.getPort(), apiManagerUrl.getScheme()), basicAuth );
		clientContext = HttpClientContext.create();
		clientContext.setAuthCache(authCache);
		SSLContextBuilder builder = SSLContextBuilder.create();
		builder.loadTrustMaterial(null, new TrustAllStrategy());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new NoopHostnameVerifier());
		 httpClient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider)
				.setSSLSocketFactory(sslsf)
				.build();
	}

	public CloseableHttpResponse get(String request) throws IOException, URISyntaxException {
		URIBuilder uri = new URIBuilder(apiManagerUrl + API_VERSION + request);
		HttpGet httpGet = new HttpGet(uri.toString());
		return httpClient.execute(httpGet, clientContext);
	}

}
