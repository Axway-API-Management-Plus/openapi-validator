package com.axway.openapi.validator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.axway.openapi.validator.Utils.TraceLevel;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.QueryStringHeaderSet;

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
}
