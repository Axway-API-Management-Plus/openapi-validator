package com.axway.apim.openapi.validator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.axway.apim.openapi.validator.Utils.TraceLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vordel.api.common.BadRequestException;

public class APIManagerSchemaProvider {
	
	APIManagerHttpClient apiManager;
	
	ObjectMapper mapper = new ObjectMapper();
	
	private boolean useOriginalAPISpec = false;
	
	public APIManagerSchemaProvider(String apiManagerUrl, String username, String password) throws Exception {
		super();
		this.apiManager = new APIManagerHttpClient(apiManagerUrl, username, password);
	}
	
	public String getSchema(String apiId) throws Exception {
		/**** debug seb*/
		Utils.traceMessage("Getschema debug seb", TraceLevel.INFO);
		if(!useOriginalAPISpec) {
			/**** debug seb*/
			Utils.traceMessage("call getFrontendAPISpec debug seb", TraceLevel.INFO);
			return getFrontendAPISpec(apiId);
		} else {
			// Get the Backend-API-ID based on the given Frontend-API ID
			/**** debug seb*/
			Utils.traceMessage("call getBackendAPIID debug seb", TraceLevel.INFO);
			String backendApiID = getBackendAPIID(apiId);
			Utils.traceMessage("Loading the original imported API specification from the API Manager using backend API-ID: " + backendApiID, TraceLevel.INFO);
			return getBackendAPISpec(backendApiID);
		}
	}

	private String getFrontendAPISpec(String apiId) throws Exception {
		/**** debug seb*/
		Utils.traceMessage("getFrontendAPISpec debug seb", TraceLevel.INFO);
		
		CloseableHttpResponse httpResponse = null;
		try {
			/**** debug seb*/
			Utils.traceMessage("getFrontendAPISpec debug seb inside try", TraceLevel.INFO);
			httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=2.0");
			
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			
			String response = EntityUtils.toString(httpResponse.getEntity());
			/**** debug seb*/
			Utils.traceMessage("getFrontendAPISpec debug seb response from 2.0 call" + response + " code : " + statusCode, TraceLevel.INFO);
			if(statusCode!=200) {
				Utils.traceMessage("getFrontendAPISpec debug seb not 200" , TraceLevel.INFO);
				if(statusCode==500 ){
					Utils.traceMessage("getFrontendAPISpec debug seb code = 500" , TraceLevel.INFO);
					if(response!=null){
						Utils.traceMessage("getFrontendAPISpec debug seb response != null" , TraceLevel.INFO);
					  if (response.contains("Failed to download API")){
						Utils.traceMessage("getFrontendAPISpec debug seb response.contains" , TraceLevel.INFO);
						 // Try to download an OAS 3.0 spec instead
						Utils.traceMessage("No SwaggerVersion 2.0 API-Specification found, trying to download OpenAPI 3.0 specification.", TraceLevel.DEBUG);
						httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=3.0");
						statusCode = httpResponse.getStatusLine().getStatusCode();
						response = EntityUtils.toString(httpResponse.getEntity());
						if(statusCode==200) { 
							return response;
						}
					  }
					}  
					                                                       
					
				}
				Utils.traceMessage("Error getting API-Specification from API-Manager. Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
				throw new IllegalArgumentException("Error getting API-Specification from API-Manager.");
			}
			return response;
		//} catch (Exception ex){
		//	int statusCode = httpResponse.getStatusLine().getStatusCode();
		//	Utils.traceMessage("getFrontendAPISpec debug seb statuscode" + statusCode, TraceLevel.INFO);
			//String response = EntityUtils.toString(httpResponse.getEntity());
			// Try to download an OAS 3.0 spec instead
			//Utils.traceMessage("No SwaggerVersion 2.0 API-Specification found, trying to download OpenAPI 3.0 specification.", TraceLevel.DEBUG);
			//httpResponse = apiManager.get("/discovery/swagger/api/id/"+apiId+"?swaggerVersion=3.0");
			//statusCode = httpResponse.getStatusLine().getStatusCode();
			//response = EntityUtils.toString(httpResponse.getEntity());
			//if(statusCode==200) { 
			//	return response;
			//}
			//Utils.traceMessage("Error getting API-Specification from API-Manager. Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
			//throw new IllegalArgumentException("Error getting API-Specification from API-Manager.");
		} finally {
			httpResponse.close();
		}
	}
	
	private String getBackendAPISpec(String backendApiId) throws Exception {
		CloseableHttpResponse httpResponse = apiManager.get("/apirepo/"+backendApiId+"/download?original=true");
		/*** debug seb */
		Utils.traceMessage("getBackendAPISpec debug seb", TraceLevel.INFO);
		try {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity());
			if(statusCode!=200) {
				Utils.traceMessage("Error getting originally imported API-Specification from API-Manager. Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
				throw new IllegalArgumentException("Error getting originally imported API-Specification from API-Manager.");
			}
			return response;
		} finally {
			httpResponse.close();
		}
	}
	
	private String getBackendAPIID(String apiId) throws Exception {
		/*** debug seb */
		Utils.traceMessage("getBackendAPIID debug seb", TraceLevel.INFO);
		CloseableHttpResponse httpResponse = apiManager.get("/proxies/"+apiId);
		
		try {
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String response = EntityUtils.toString(httpResponse.getEntity());
			if(statusCode!=200) {
				if(statusCode==403 && response!=null && response.contains("Forbidden")) {
					Utils.traceMessage("Please check the given API-ID: '"+apiId+"' is correct, as the API-Manager REST-API returns forbidden for an incorrect API-ID.", TraceLevel.INFO);
				}
				Utils.traceMessage("Error loading Backend-API-ID from API Manager for API: "+apiId+". Received Status-Code: " +statusCode+ ", Response: '" + response + "'", TraceLevel.ERROR);
				throw new IllegalArgumentException("Error loading Backend-API-ID from API Manager for API: " + apiId);
			}
			JsonNode node = mapper.readTree(response);
			return node.get("apiId").asText();
		} finally {
			httpResponse.close();
		}
	}

	public boolean isUseOriginalAPISpec() {
		return useOriginalAPISpec;
	}

	public void setUseOriginalAPISpec(boolean useOriginalAPISpec) {
		this.useOriginalAPISpec = useOriginalAPISpec;
	}
}
