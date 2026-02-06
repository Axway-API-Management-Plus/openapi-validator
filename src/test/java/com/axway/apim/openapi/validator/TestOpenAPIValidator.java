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
    public void invalidGetRequestPetsByStatusWithoutQueryParam() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	String path = "/pet/findByStatus";
    	QueryStringHeaderSet queryParams = new QueryStringHeaderSet();
    	String verb = "GET";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertFalse(validator.isValidRequest(null, verb, path, queryParams, headers), "Request is invalid!");
    }

    @Test
    public void validRequestWithFullPathAndPathParameter() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	validator.getExposurePath2SpecifiedPathMap().setMaxSize(2);

    	String path = "/api/petstore/v3/store/order/1234";
    	String verb = "DELETE";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");
    	// Run it a second time, which should use the cached path
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");

    	path = "/api/petstore/v3/store/order/5345";
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");

    	path = "/api/petstore/v3/store/order/434312";
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");

    	path = "/api/petstore/v3/store/order/978978";
    	Assert.assertTrue(validator.isValidRequest(null, verb, path, null, headers), "Request should be valid!");

    	// Check the cache
    	Assert.assertEquals(validator.getExposurePath2SpecifiedPathMap().size(), 2, "Cached paths should be two as the size is limited. "
    			+ "Cached paths: " + validator.getExposurePath2SpecifiedPathMap().toString());
    }

    @Test
    public void invalidRequestWithFullPathAndPathParameter() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	String path = "/api/petstore/v3/store/order/invalidParameter";
    	String verb = "DELETE";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertFalse(validator.isValidRequest(null, verb, path, null, headers));
    }

    @Test
    public void invalidRequestNoMatchToSpec() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	String path = "/api/petstore/will/never/map/should/be/cached/anyway";
    	String verb = "GET";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertFalse(validator.isValidRequest(null, verb, path, null, headers));
    	// This time, it should be cached anyway
    	Assert.assertFalse(validator.isValidRequest(null, verb, path, null, headers));
    }

    @Test
    public void invalidNoMatch2SpecAtAll() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	String path = "/no/match";
    	String verb = "GET";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertFalse(validator.isValidRequest(null, verb, path, null, headers));
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
    public void validResponsewithoutBody()
    {
    	OpenAPIValidator validator = OpenAPIValidator.getInstance("https://petstore.swagger.io/v2/swagger.json");

    	String path = "/pet";
    	String verb = "POST";
    	int status = 200;
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");

    	Assert.assertFalse(validator.isValidResponse(null, verb, path, status, headers), "Request should be not valid!");
    }

    @Test
    public void testSpecialCharsQueryParam() throws IOException
    {
    	String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
    	OpenAPIValidator validator = OpenAPIValidator.getInstance(swagger);

    	String path = "/user/login";
    	String verb = "GET";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
    	QueryStringHeaderSet queryParams = new QueryStringHeaderSet();
    	queryParams.addHeader("username", "otr%C3%B3s");
    	queryParams.addHeader("password", "otr�s");

    	Assert.assertTrue(validator.isValidRequest(null, verb, path, queryParams, headers));

    	validator.setDecodeQueryParams(false);

    	QueryStringHeaderSet queryParams2 = new QueryStringHeaderSet();
    	queryParams2.addHeader("username", "otr%C3%B3s");
    	queryParams2.addHeader("password", "otr�s");

    	Assert.assertFalse(validator.isValidRequest(null, verb, path, queryParams2, headers));
    }


}
