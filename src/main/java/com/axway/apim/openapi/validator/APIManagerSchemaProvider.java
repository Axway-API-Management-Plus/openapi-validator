package com.axway.apim.openapi.validator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.axway.apim.openapi.validator.Utils.TraceLevel;

public class APIManagerSchemaProvider {
	
	APIManagerHttpClient apiManager;
	
	public APIManagerSchemaProvider(String apiManagerUrl, String username, String password) throws Exception {
		super();
		this.apiManager = new APIManagerHttpClient(apiManagerUrl, username, password);
	}

	public String getSchema(String apiId) throws Exception {
		
		CloseableHttpResponse httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=2.0");
		
		try {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity());
			if(statusCode!=200) {
				Utils.traceMessage("Error getting API-Specification from API-Manager. Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
				throw new IllegalArgumentException("Error getting API-Specification from API-Manager");
			}
			return response;
		} finally {
			httpResponse.close();
		}
	}
}
