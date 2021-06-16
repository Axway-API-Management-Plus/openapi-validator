package com.axway.openapi.validator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest.Builder;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.axway.openapi.validator.Utils.TraceLevel;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.QueryStringHeaderSet;
//import com.vordel.mime.JSONBody;

enum HttpMethod {
	GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
}

/**
 * OpenAPIValidator
 *
 */
public class OpenAPIValidator
{
	private static Map<Integer, OpenAPIValidator> instances = new HashMap<Integer, OpenAPIValidator>();
	
	private OpenApiInteractionValidator validator;
	
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
    
    private OpenAPIValidator(String openAPISpec) {
		super();
		try {
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
    
    /*public boolean isValidRequest(JSONBody body, String verb, String path, HeaderSet headers) throws IOException {
    	String payload = body.getJSON().asText();
    	return isValidRequest(payload, verb, path, headers);
    }*/
    
    public boolean isValidRequest(String payload, String verb, String path, QueryStringHeaderSet queryParams, HeaderSet headers) {
    	Utils.traceMessage("Validate request: [verb: "+verb+", path: '"+path+"', payload: '"+Utils.getContentStart(payload, 20, true)+"']", TraceLevel.INFO);
    	ValidationReport validationReport = validateRequest(payload, verb, path, queryParams, headers);
    	if (validationReport.hasErrors()) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean isValidResponse(String payload, String verb, String path, int status, HeaderSet headers) {
    	Utils.traceMessage("Validate response: [verb: "+verb+", path: '"+path+"', status: +status+, payload: '"+Utils.getContentStart(payload, 20, true)+"']", TraceLevel.INFO);
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
				return Utils.headersToMap(headers);
			}
			
			@Override
			public Collection<String> getHeaderValues(String name) {
				return Utils.getHeaderValues(headers, name);
			}
		};
		
    	//Builder requestBuilder = getBuilderForMethod(verb, path);
    	//Request request = addRequestHeaders(requestBuilder, headers).withBody(payload).build();
    	ValidationReport validationReport = validator.validateRequest(request);
    	if(validationReport.hasErrors()) {
    		for(Message message : validationReport.getMessages()) {
    			System.out.println(message.getMessage());
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
    			// System.out.println(message.getMessage());
    			Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
    		}
    	}
    	return validationReport;
    }
    
    /*private Builder getBuilderForMethod(String verb, String path) {
    	HttpMethod method = HttpMethod.valueOf(verb);
    	switch (method) {
		case OPTIONS: 
			return SimpleRequest.Builder.options(path);
		case GET: 
			return SimpleRequest.Builder.get(path);
		case POST: 
			return SimpleRequest.Builder.post(path);
		case PUT: 
			return SimpleRequest.Builder.put(path);
		case PATCH: 
			return SimpleRequest.Builder.patch(path);
		case DELETE: 
			return SimpleRequest.Builder.delete(path);
		case HEAD: 
			return SimpleRequest.Builder.head(path);
		default:
			throw new IllegalArgumentException("Unexpected http verb: " + verb);
		}
    }*/
    
    private Builder addRequestHeaders(Builder builder, HeaderSet headers) {
    	boolean contentTypeHeaderFound = false;
    	Iterator<String> it = headers.getHeaderNames();
    	while(it.hasNext()) {
    		String headerName = it.next();
    		if(headerName.toLowerCase().equals("Content-Type")) contentTypeHeaderFound=true;
    		String headerValue = headers.getHeader(headerName);
    		builder.withHeader(headerName, headerValue);
    	}
    	if(!contentTypeHeaderFound) {
    		Utils.traceMessage("Content-Type header is missing, assuming application/json", TraceLevel.ERROR);
    		builder.withHeader("Content-Type", "application/json");
    	}
    	return builder;
    }
}
