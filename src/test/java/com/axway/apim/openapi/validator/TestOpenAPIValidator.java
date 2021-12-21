package com.axway.apim.openapi.validator;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Level;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.QueryStringHeaderSet;

/**
 * Unit test for the OpenAPI Validator
 */
public class TestOpenAPIValidator 
{
	
	private static final String TEST_PACKAGE = "com/axway/apim/openapi/validator/";

    @Test
    public void invalidPostRequestPetstoreSwagger20() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = "Some invalid payload";
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	ValidationReport report = validator.validateRequest(payload, verb, path, null, headers);
    	report.getMessages().get(0).getMessage();
    	
    	Assert.assertTrue(report.hasErrors(), "Request should be not valid!");
    	Assert.assertEquals(report.getMessages().size(), 1, "One Error message expected");
    	Assert.assertEquals(report.getMessages().get(0).getKey(), "validation.request.body.schema.invalidJson");
    	Assert.assertEquals(report.getMessages().get(0).getLevel(), Level.ERROR);
    }
    
    @Test
    public void isValidRequestFalseTest() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "InvalidPostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertFalse(validator.isValidRequest(payload, verb, path, null, headers));
    }
    
    @Test
    public void validPostRequestPetstoreSwagger20() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidPostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidRequest(payload, verb, path, null, headers), "Request should be not valid!");
    }
    
    @Test
    public void validGetRequestPetsByStatusPetstoreSwagger20() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String path = "/pet/findByStatus";
    	QueryStringHeaderSet queryParams = new QueryStringHeaderSet();
    	queryParams.addHeader("status", "pending");
    	String verb = "GET";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, queryParams, headers), "Request should be valid!");
    }
    
    @Test
    public void validDeleteRequestOrderBasedOnId() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String path = "/store/order/1234";
    	String verb = "DELETE";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");
    }
    
    @Test
    public void validRequestExternalURLSwagger20() throws IOException
    {
    	OpenAPIValidator validator = OpenAPIValidator.getInstance("https://petstore.swagger.io/v2/swagger.json");
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidPostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidRequest(payload, verb, path, null, headers), "Request should be not valid!");
    }
    
    @Test
    public void invalidPostRequestNoContentTypeHeader() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);
    	
    	String payload = "Some invalid payload";
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	
    	Assert.assertFalse(validator.isValidRequest(payload, verb, path, null, headers));
    }
    
    @Test
    public void validResponse() throws IOException
    {
    	OpenAPIValidator validator = OpenAPIValidator.getInstance("https://petstore.swagger.io/v2/swagger.json");
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidGetPetFindByStatusResponse.json"));
    	String path = "/pet/findByStatus";
    	String verb = "GET";
    	int status = 200;
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertTrue(validator.isValidResponse(payload, verb, path, status, headers), "Request should be not valid!");
    }
    
    @Test
    public void invalidResponse() throws IOException
    {
    	OpenAPIValidator validator = OpenAPIValidator.getInstance("https://petstore.swagger.io/v2/swagger.json");
    	
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "InvalidGetPetFindByStatusResponse.json"));
    	String path = "/pet/findByStatus";
    	String verb = "GET";
    	int status = 200;
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertFalse(validator.isValidResponse(payload, verb, path, status, headers), "Request should be not valid!");
    }
    
    @Test
    public void validResponsewithoutBody() throws IOException
    {
    	OpenAPIValidator validator = OpenAPIValidator.getInstance("https://petstore.swagger.io/v2/swagger.json");
    	
    	String path = "/pet";
    	String verb = "POST";
    	int status = 200;
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	
    	Assert.assertFalse(validator.isValidResponse(null, verb, path, status, headers), "Request should be not valid!");
    }
    
}
