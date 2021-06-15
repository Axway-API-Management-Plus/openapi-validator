package com.axway.apim.openapi.validator;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Level;
import com.axway.openapi.validator.OpenAPIValidator;
import com.vordel.mime.HeaderSet;

/**
 * Unit test for the OpenAPI Validator
 */
public class OpenAPIValidatorTest 
{
	
	private static final String TEST_PACKAGE = "com/axway/apim/openapi/validator/";

    @Test
    public void invalidPostPetstoreSwagger20() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = "Some invalid payload";
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	ValidationReport report = validator.validateRequest(payload, verb, path, headers);
    	report.getMessages().get(0).getMessage();
    	
    	Assert.assertTrue(report.hasErrors(), "Request should be not valid!");
    	Assert.assertEquals(report.getMessages().size(), 1, "One Error message expected");
    	Assert.assertEquals(report.getMessages().get(0).getKey(), "validation.request.body.schema.invalidJson");
    	Assert.assertEquals(report.getMessages().get(0).getLevel(), Level.ERROR);
    }
    
    @Test
    public void validPostPetstoreSwagger20() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidRequest(payload, verb, path, headers), "Request should be not valid!");
    }
}
