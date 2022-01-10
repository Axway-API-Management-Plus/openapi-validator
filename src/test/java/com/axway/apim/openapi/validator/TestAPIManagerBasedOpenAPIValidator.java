package com.axway.apim.openapi.validator;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.JsonBody;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import com.vordel.mime.HeaderSet;

public class TestAPIManagerBasedOpenAPIValidator {
	
	private ClientAndServer mockServer;
	
	private static final String TEST_PACKAGE = "com/axway/apim/openapi/validator/";
	
	@SuppressWarnings("resource")
	@Test
	public void getAPISpecForValidApiId() throws Exception {
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/discovery/swagger/api/id/d45571e4-ec7d-444c-af09-11265d75c446")
				.withQueryStringParameter("swaggerVersion", "2.0")
				)
		.respond(response().withBody("{ This is supposed to be a swagger-file }")
		);
		APIManagerSchemaProvider provider = new APIManagerSchemaProvider("https://localhost:1080", "user", "password");
		String apiSpec = provider.getSchema("d45571e4-ec7d-444c-af09-11265d75c446");
		Assert.assertEquals(apiSpec, "{ This is supposed to be a swagger-file }");
	}
	
	@SuppressWarnings("resource")
	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Error getting API-Specification from API-Manager.")
	public void getAPISpecForInvalidApiId() throws Exception {
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/discovery/swagger/api/id/d45571e4-ec7d-444c-af09-99999999999")
				.withQueryStringParameter("swaggerVersion", "2.0")
				)
		.respond(response()
				.withStatusCode(400)
				.withBody(new JsonBody("{\"errors\":[{\"code\":400,\"message\":\"Unknown API\"}]}"))
		);
		APIManagerSchemaProvider provider = new APIManagerSchemaProvider("https://localhost:1080", "user", "password");
		provider.getSchema("d45571e4-ec7d-444c-af09-99999999999");
	}
	
	@SuppressWarnings("resource")
	@Test
	public void getAPISpecForValidOAS30ApiId() throws Exception {
		String failedToDownloadAPI = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "FailedToDownloadAPI.json"));
		// Initially trying to download Swagger 2.0
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/discovery/swagger/api/id/d45571e4-ec7d-444c-af09-11265d75c446")
				.withQueryStringParameter("swaggerVersion", "2.0"))
			.respond(response().withStatusCode(500).withBody(new JsonBody(failedToDownloadAPI))
				);
		mock.when(request()
				.withPath("/api/portal/v1.3/discovery/swagger/api/id/d45571e4-ec7d-444c-af09-11265d75c446")
				.withQueryStringParameter("swaggerVersion", "3.0")
				)
		.respond(response().withStatusCode(200).withBody("{ This is supposed to be a swagger-file }")
		);

		APIManagerSchemaProvider provider = new APIManagerSchemaProvider("https://localhost:1080", "user", "password");
		String apiSpec = provider.getSchema("d45571e4-ec7d-444c-af09-11265d75c446");
		Assert.assertEquals(apiSpec, "{ This is supposed to be a swagger-file }");
	}
	
	@SuppressWarnings("resource")
	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Error loading Backend-API-ID from API Manager for API: INVALID-API-ID")
	public void getBackendAPISpecForInvalidApiId() throws Exception {
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/proxies/INVALID-API-ID"))
			.respond(response().withStatusCode(403).withBody(new JsonBody("{\"errors\":[{\"code\":403,\"message\":\"Forbidden\"}]}"))
		);

		APIManagerSchemaProvider provider = new APIManagerSchemaProvider("https://localhost:1080", "user", "password");
		provider.setUseOriginalAPISpec(true);
		String apiSpec = provider.getSchema("INVALID-API-ID");
		Assert.assertEquals(apiSpec, "{ This is supposed to be a swagger-file }");
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testOpenAPISpecification() throws Exception {
		String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "PetstoreSwagger2.0.json"));
		
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/discovery/swagger/api/id/d45571e4-ec7d-444c-af09-11265d75c888")
				.withQueryStringParameter("swaggerVersion", "2.0"))
			.respond(response().withStatusCode(200).withBody(new JsonBody(swagger))
		);
		OpenAPIValidator validator = OpenAPIValidator.getInstance("d45571e4-ec7d-444c-af09-11265d75c888", "user", "password", "https://localhost:1080");
		
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidPostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
		
		boolean isValid = validator.isValidRequest(payload, verb, path, null, headers);
		
		Assert.assertTrue(isValid, "Request is supposed to be valid");
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testOriginalAPISpecification() throws Exception {
		String apiProxy = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "FrontAPIProxyResponse.json"));
		String swagger = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "OriginallyImportedAPISpecification.json"));
		
		MockServerClient mock = new MockServerClient("127.0.01", 1080);
		mock.when(request()
				.withPath("/api/portal/v1.3/proxies/d45571e4-ec7d-444c-af09-11265d75c888") )
		.respond(response()
				.withStatusCode(200)
				.withBody(new JsonBody(apiProxy)));
		mock.when(request()
				.withPath("/api/portal/v1.3/apirepo/ab9e6a41-cb93-4a9b-9faf-74a901f14922/download")
				.withQueryStringParameter("original", "true"))
		.respond(response()
				.withStatusCode(200)
				.withBody(new JsonBody(swagger))
			);
		OpenAPIValidator validator = OpenAPIValidator.getInstance("d45571e4-ec7d-444c-af09-11265d75c888", "user", "password", "https://localhost:1080", true);
		
    	String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidPostPetPayload.json"));
    	String path = "/pet";
    	String verb = "POST";
    	HeaderSet headers = new HeaderSet();
    	headers.addHeader("Content-Type", "application/json");
		
		boolean isValid = validator.isValidRequest(payload, verb, path, null, headers);
		
		Assert.assertTrue(isValid, "Request is supposed to be valid");
	}

	@BeforeClass
	public void beforeClass() {
		mockServer = startClientAndServer(1080);
		ConfigurationProperties.logLevel("WARNING");
	}

	@AfterClass
	public void afterClass() {
		mockServer.stop();
	}

}
