package com.axway.openapi.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vordel.trace.Trace;

public class Utils {
	
	static Logger LOG = LoggerFactory.getLogger("OpenAPIValidator");
	
	enum TraceLevel {
		FATAL, ALWAYS, ERROR, INFO, MIN, DEBUG, DATA ;
	}
	
	private static boolean useAPIGatewayTrace = true;
	
	public static void traceMessage(String message, TraceLevel level) {
		traceMessage(message, null, level);
	}
	
	public static void traceMessage(String message, Throwable t, TraceLevel level) {
		setupTraceMode();
		switch (level) {
		case FATAL: 
			if(useAPIGatewayTrace) { Trace.fatal(message, t); } else { LOG.error(message, t); }
			break;
		case ALWAYS: 
			if(useAPIGatewayTrace) { Trace.always(message, t); } else { LOG.info(message, t); }
			break;
		case ERROR:
			if(useAPIGatewayTrace) { Trace.error(message, t); } else { LOG.error(message, t); }
			break;
		case INFO:
			if(useAPIGatewayTrace) { Trace.info(message, t); } else { LOG.info(message, t); }
			break;
		case MIN: 
			if(useAPIGatewayTrace) { Trace.min(message, t); } else { LOG.warn(message, t); }
			break;
		case DEBUG:
			if(useAPIGatewayTrace) { Trace.debug(message, t); } else { LOG.debug(message, t); }
			break;
		case DATA:
			if(useAPIGatewayTrace) { Trace.trace(message, Trace.TRACE_DATA); } else { LOG.trace(message, t); }
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
	
	public static String getContentStart(String someContent) {
		try {
			if(someContent == null) return "N/A";
			return (someContent.length()<200) ? someContent.substring(0, someContent.length()) : someContent.substring(0, 200) + "...";
		} catch (Exception e) {
			return "Cannot get content from API-Specification. " + e.getMessage();
		}		
	}
}
