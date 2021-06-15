package com.axway.openapi.validator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleRequest.Builder;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.axway.openapi.validator.Utils.TraceLevel;
import com.vordel.mime.HeaderSet;

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
		this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(openAPISpec).build();
	}
    
    public boolean isValidRequest(String payload, String verb, String path, HeaderSet headers) {
    	ValidationReport validationReport = validateRequest(payload, verb, path, headers);
    	if (validationReport.hasErrors()) {
    		return false;
    	} else {
    		return true;
    	}
    }

	public ValidationReport validateRequest(String payload, String verb, String path, HeaderSet headers) {
    	Builder requestBuilder = getBuilderForMethod(verb, path);
    	Request request = addRequestHeaders(requestBuilder, headers).withBody(payload).build();
    	ValidationReport validationReport = validator.validateRequest(request);
    	if(validationReport.hasErrors()) {
    		for(Message message : validationReport.getMessages()) {
    			Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
    		}
    	}
    	return validationReport;
    }
    
    private Builder getBuilderForMethod(String verb, String path) {
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
    }
    
    private Builder addRequestHeaders(Builder requestBuilder, HeaderSet headers) {
    	Iterator<String> it = headers.getHeaderNames();
    	while(it.hasNext()) {
    		String headerName = it.next();
    		String headerValue = headers.getHeader(headerName);
    		requestBuilder.withHeader(headerName, headerValue);
    	}
    	return requestBuilder;
    }
}
