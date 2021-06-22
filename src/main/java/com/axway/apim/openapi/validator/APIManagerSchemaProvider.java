package com.axway.apim.openapi.validator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.axway.apim.openapi.validator.Utils.TraceLevel;

public class APIManagerSchemaProvider {
	
	APIManagerHttpClient apiManager;
	
	public APIManagerSchemaProvider(String apiManagerUrl, String username, String password) throws Exception {
		super();
		Utils.traceMessage("Creating API-Manager OpenAPI provider for URL: "+apiManagerUrl, TraceLevel.INFO);
		this.apiManager = new APIManagerHttpClient(apiManagerUrl, username, password);
	}

	public String getSchema(String apiId) throws Exception {
		
		CloseableHttpResponse httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=2.0");
		
		try {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity());
			if(statusCode!=200) {
				if(statusCode==500 && response!=null && response.contains("Failed to download API")) {
					// Try to download an OAS 3.0 spec instead
					Utils.traceMessage("No SwaggerVersion 2.0 API-Specification found, trying to download OpenAPI 3.0 specification.", TraceLevel.DEBUG);
					httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=3.0");
					statusCode = httpResponse.getStatusLine().getStatusCode();
					response = EntityUtils.toString(httpResponse.getEntity());
					if(statusCode==200) { 
						return response;
					}
				}
				Utils.traceMessage("Error getting API-Specification from API-Manager. Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
				throw new IllegalArgumentException("Error getting API-Specification from API-Manager");
			}
			return response;
		} finally {
			httpResponse.close();
		}
	}
}
