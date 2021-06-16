package com.axway.openapi.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vordel.mime.HeaderSet;
import com.vordel.mime.HeaderSet.Header;
import com.vordel.mime.HeaderSet.HeaderEntry;
import com.vordel.trace.Trace;

public class Utils {
	
	private static Logger LOG = LoggerFactory.getLogger("OpenAPIValidator");
	private static String LOG_PREFIX = "OpenAPIValidator: ";
	
	enum TraceLevel {
		FATAL, ALWAYS, ERROR, INFO, MIN, DEBUG, DATA ;
	}
	
	private static boolean useAPIGatewayTrace = true;
	
	public static void traceMessage(String message, TraceLevel level) {
		traceMessage(message, null, level);
	}
	
	public static void traceMessage(String message, Throwable t, TraceLevel level) {
		setupTraceMode();
		if(level==null) level = TraceLevel.ERROR;
		switch (level) {
		case FATAL: 
			if(useAPIGatewayTrace) { Trace.fatal(LOG_PREFIX + message, t); } else { LOG.error(message, t); }
			break;
		case ALWAYS: 
			if(useAPIGatewayTrace) { Trace.always(LOG_PREFIX + message, t); } else { LOG.info(message, t); }
			break;
		case ERROR:
			if(useAPIGatewayTrace) { Trace.error(LOG_PREFIX + message, t); } else { LOG.error(message, t); }
			break;
		case INFO:
			if(useAPIGatewayTrace) { Trace.info(LOG_PREFIX + message, t); } else { LOG.info(message, t); }
			break;
		case MIN: 
			if(useAPIGatewayTrace) { Trace.min(LOG_PREFIX + message, t); } else { LOG.warn(message, t); }
			break;
		case DEBUG:
			if(useAPIGatewayTrace) { Trace.debug(LOG_PREFIX + message, t); } else { LOG.debug(message, t); }
			break;
		case DATA:
			if(useAPIGatewayTrace) { Trace.trace(LOG_PREFIX + message, Trace.TRACE_DATA); } else { LOG.trace(message, t); }
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + level);
		}
	}
	
	private static void setupTraceMode() {
		try {
			Trace.debug("");
		} catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
			useAPIGatewayTrace = false;
		} catch (Exception ignore) {}; 
	}
	
	public static String getContentStart(String someContent, int maxLength, boolean removeNewLines) {
		try {
			if(someContent == null) return "N/A";
			if(removeNewLines) someContent = someContent.replaceAll("\\r?\\n", "");
			return (someContent.length()<maxLength) ? someContent.substring(0, someContent.length()) : someContent.substring(0, maxLength) + "...(truncated)";
		} catch (Exception e) {
			return "Cannot get content from API-Specification. " + e.getMessage();
		}		
	}
    
    public static Collection<String> getHeaderValues(HeaderSet headers, String name) {
    	Collection<String> result = new ArrayList<String>();
    	if(headers==null) return result;
		HeaderEntry headerValues = headers.getHeaderEntry(name);
		if(headerValues==null) {
			if(name.toLowerCase().equals("content-type")) {
				traceMessage("Header: Content-Type not found. Defaulting to application/json", TraceLevel.DEBUG);
				result.add("application/json");
				return result;
			} else {
				return result; // Header might not be set
			}
		}
		Iterator<Header> it = headerValues.iterator();
		while(it.hasNext()) {
			Header header = it.next();
			result.add(header.toString());
		}
		return result;
    }
}
