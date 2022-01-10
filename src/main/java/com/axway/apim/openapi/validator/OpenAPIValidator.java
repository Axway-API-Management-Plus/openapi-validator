package com.axway.apim.openapi.validator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.OpenApiInteractionValidator.ApiLoadException;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.axway.apim.openapi.validator.Utils.TraceLevel;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.QueryStringHeaderSet;

/**
 * OpenAPIValidator
 *
 */
public class OpenAPIValidator
{
	private static Map<Integer, OpenAPIValidator> instances = new HashMap<Integer, OpenAPIValidator>();
	
	private static Map<String, OpenAPIValidator> instances4APIIDs = new HashMap<String, OpenAPIValidator>();
	
	private OpenApiInteractionValidator validator;
	
	private MaxSizeHashMap<String, Object> exposurePath2SpecifiedPathMap = new MaxSizeHashMap<String, Object>();
	
	private int payloadLogMaxLength = 40;
	
	public static synchronized OpenAPIValidator getInstance(String openAPISpec)  {
		int hashCode = openAPISpec.hashCode();
		if(instances.containsKey(hashCode)) {
			Utils.traceMessage("Using existing OpenAPIValidator instance for API-Specification.", TraceLevel.INFO);
			return instances.get(hashCode);
		} else {
			Utils.traceMessage("Creating new OpenAPI validator instance for given API-Specification.", TraceLevel.INFO);
			OpenAPIValidator validator = new OpenAPIValidator(openAPISpec);
			instances.put(hashCode, validator);
			return validator;
		}
	}
	
	public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, boolean useOriginalAPISpec) throws Exception  {
		String apiManagerURL = "https://localhost:8075";
		return getInstance(apiId, username, password, apiManagerURL, useOriginalAPISpec);
	}
	
	public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, String apiManagerUrl) throws Exception  {
		return getInstance(apiId, username, password, apiManagerUrl, false);
	}
	
	public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password) throws Exception  {
		String apiManagerURL = "https://localhost:8075";
		return getInstance(apiId, username, password, apiManagerURL, false);
	}
	
	public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, String apiManagerUrl, boolean useOriginalAPISpec) throws Exception  {
		if(instances4APIIDs.containsKey(apiId)) {
			Utils.traceMessage("Using cached instance of OpenAPI validator for API-ID: " + apiId, TraceLevel.DEBUG);
			return instances4APIIDs.get(apiId);
		} else {
			OpenAPIValidator validator = new OpenAPIValidator(apiId, username, password, apiManagerUrl, useOriginalAPISpec);
			instances4APIIDs.put(apiId, validator);
			Utils.traceMessage("Returning created OpenAPI validator for API-ID: " + apiId, TraceLevel.DEBUG);
			return validator;
		}
	}
    
    private OpenAPIValidator(String openAPISpec) {
		super();
		try {
			exposurePath2SpecifiedPathMap.setMaxSize(1000);
			// Check if openAPISpec is a valid URL
			new URI(openAPISpec);
			// If it does not fail, load it from the URL
			Utils.traceMessage("Creating OpenAPIValidator from URL specification: " + openAPISpec, TraceLevel.INFO);
			this.validator = OpenApiInteractionValidator.createForSpecificationUrl(openAPISpec).build();
		} catch (Exception e) {
			Utils.traceMessage("Creating OpenAPIValidator from inline specification", TraceLevel.INFO);
			this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(openAPISpec).build();
		}
	}
    
    private OpenAPIValidator(String apiId, String username, String password, String apiManagerUrl, boolean useOriginalAPISpec) throws Exception {
		super();
		try {
			Utils.traceMessage("Creating OpenAPIValidator for: [apiId: '"+apiId+"', username: '"+username+"', password: '*******', apiManagerUrl: '"+apiManagerUrl+"']", TraceLevel.INFO);
			APIManagerSchemaProvider schemaProvider = new APIManagerSchemaProvider(apiManagerUrl, username, password);
			schemaProvider.setUseOriginalAPISpec(useOriginalAPISpec);
			String apiSpecification = schemaProvider.getSchema(apiId);
			this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(apiSpecification).build();
		} catch (ApiLoadException e) {
			Utils.traceMessage("The obtained API-Specification is not compatible with the OpenAPI validator.", e, TraceLevel.ERROR);
		} catch (Exception e) {
			Utils.traceMessage("Error creating OpenAPIValidator for given API-ID: "+apiId, e, TraceLevel.ERROR);
			throw e;
		}
	}
    
    public boolean isValidRequest(String payload, String verb, String path, QueryStringHeaderSet queryParams, HeaderSet headers) {
    	Utils.traceMessage("Validate request: [verb: "+verb+", path: '"+path+"', payload: '"+Utils.getContentStart(payload, payloadLogMaxLength, true)+"']", TraceLevel.INFO);
    	ValidationReport validationReport = null;
    	String originalPath = path;
    	// The given path might be the FE-API exposure path which is not guaranteed to be part of the API-Specification.
    	// If for example the Petstore is exposed with /petstore/v3 the path we get might be /petstore/v3/store/order/78787 
    	// and with that, the validation fails, as the specification doesn't contain this path.
    	// The following code nevertheless tries to find the matching API path by removing the first part of the path and 
    	// repeating the process max. 5 times. 
    	
    	boolean cachePath = false;
    	// Perhaps the path has already looked up and matched to the specified path (e.g. /petstore/v3/store/order/78787 --> /store/order/{orderId} 
    	if(exposurePath2SpecifiedPathMap.containsKey(path)) {
    		if(exposurePath2SpecifiedPathMap.get(path) instanceof ValidationReport) {
    			// Previously with the given we got a validation error, just return the same again
    			validationReport = (ValidationReport)exposurePath2SpecifiedPathMap.get(path);
    		} else {
	    		// In that case perform the validation based on the cached path
	    		validationReport = validateRequest(payload, verb, (String)exposurePath2SpecifiedPathMap.get(path), queryParams, headers);
    		}
    	} else {
    		// Otherwise try to find the belonging specified path
	    	for(int i=0; i<5;i++) {
	    		if(cachePath) {
	    			Utils.traceMessage("Retrying validation with reduced path: '"+path+"']' ("+i+"/5)", TraceLevel.INFO);
	    		}
	    		validationReport = validateRequest(payload, verb, path, queryParams, headers);
	    		if(validationReport.hasErrors()) {
	    			if(validationReport.getMessages().toString().contains("No API path found that matches request")) {
	    				// Only cache the path, if a direct hit fails 
	    				cachePath = true;
	    				/*
	    				 * If no match was found in the API-Spec for the given path, then we remove the first part of the 
	    				 * path because the API might be exposed with a different path by the API-Manager than defined in the spec.
	    				 * For example: /great-petstore/pet/31233 will not find anything in the first attempt, because the 
	    				 * API does not exist with /great-petstore in the API-Spec. This process is repeated at most 5 times.
	    				 */
	    				if(path.indexOf("/", 1)==-1) {
	    					break; // No need to continue as there is only a single path left (/petstore)
	    				} else {
	    					path = path.substring(path.indexOf("/", 1), path.length());
	    				}
	    			} else {
	    				break;
	    			}
	    		} else {
	    			break;
	    		}
	    	}
			if(cachePath) {
				if(validationReport.hasErrors() && validationReport.getMessages().toString().contains("No API path found that matches request")) {
					// Finally no match found, cache the returned validation report
					exposurePath2SpecifiedPathMap.put(originalPath, validationReport);
				} else {
					exposurePath2SpecifiedPathMap.put(originalPath, path);
				}
			}
    	}
    	
    	if (validationReport.hasErrors()) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean isValidResponse(String payload, String verb, String path, int status, HeaderSet headers) {
    	Utils.traceMessage("Validate response: [verb: "+verb+", path: '"+path+"', status: "+status+", payload: '"+Utils.getContentStart(payload, payloadLogMaxLength, true)+"']", TraceLevel.INFO);
    	ValidationReport validationReport = validateResponse(payload, verb, path, status, headers);
    	if (validationReport.hasErrors()) {
    		return false;
    	} else {
    		return true;
    	}
    }

	public ValidationReport validateRequest(final String payload, final String verb, final String path, final QueryStringHeaderSet queryParams, final HeaderSet headers) {
		Request request = new Request() {
			@Override
			public String getPath() {
				return path;
			}
			
			@Override
			public Method getMethod() {
				return Request.Method.valueOf(verb);
			}
			
			@Override
			public Optional<String> getBody() {
				return Optional.ofNullable(payload);
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public Collection<String> getQueryParameters() {
				if(queryParams==null) return Collections.emptyList();
				return (Collection<String>) ((queryParams.size() == 0) ? Collections.emptyList() : queryParams.getHeaderSet());
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public Collection<String> getQueryParameterValues(String name) {
				if(queryParams==null) return Collections.emptyList();
				ArrayList<String> values = queryParams.getHeaderValues(name);
				return (Collection<String>) ((values == null) ? Collections.emptyList() : values);
			}
			
			@Override
			public Map<String, Collection<String>> getHeaders() {
				// Not used for the validation
				return null;
			}
			
			@Override
			public Collection<String> getHeaderValues(String name) {
				return Utils.getHeaderValues(headers, name);
			}
		};

    	ValidationReport validationReport = validator.validateRequest(request);
    	if(validationReport.hasErrors()) {
    		for(Message message : validationReport.getMessages()) {
    			Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
    		}
    	}
    	return validationReport;
    }
	
	public ValidationReport validateResponse(final String payload, String verb, String path, final int status, final HeaderSet headers) {
		Response response = new Response() {
			@Override
			public int getStatus() {
				return status;
			}
			@Override
			public Collection<String> getHeaderValues(String name) {
				return Utils.getHeaderValues(headers, name);
			}
			@Override
			public Optional<String> getBody() {
				return Optional.ofNullable(payload);
			}
		};
    	
    	ValidationReport validationReport = validator.validateResponse(path, Request.Method.valueOf(verb), response);
    	if(validationReport.hasErrors()) {
    		for(Message message : validationReport.getMessages()) {
    			Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
    		}
    	}
    	return validationReport;
    }

	public int getPayloadLogMaxLength() {
		return payloadLogMaxLength;
	}

	public void setPayloadLogMaxLength(int payloadLogMaxLength) {
		this.payloadLogMaxLength = payloadLogMaxLength;
	}

	static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {

		private static final long serialVersionUID = 1L;

		private int maxSize; 

		@Override
		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			return size() > maxSize;
		}

		public void setMaxSize(int maxSize) {
			// Reset the cache if the maxSize is set
			clear();
			this.maxSize = maxSize;
		}
	}

	public MaxSizeHashMap<String, Object> getExposurePath2SpecifiedPathMap() {
		return exposurePath2SpecifiedPathMap;
	}
}
