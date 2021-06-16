package com.axway.apim.openapi.validator;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import com.axway.openapi.validator.Utils;

public class UtilsTest {
	
	private static final String TEST_PACKAGE = "com/axway/apim/openapi/validator/";
	
	@Test
	public void testGetContentStartShortContent() throws IOException {
		String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidPostPetPayload.json"));
		String payloadStart = Utils.getContentStart(payload, 40, true);
		Assert.assertEquals(payloadStart, "{  \"id\": 12,  \"category\": {    \"id\": 233...(truncated)");
	}
	
	@Test
	public void testGetContentStartLongContent() throws IOException {
		String payload = Files.readFile(this.getClass().getClassLoader().getResourceAsStream(TEST_PACKAGE + "ValidGetPetFindByStatusResponse.json"));
		String payloadStart = Utils.getContentStart(payload, 40, true);
		Assert.assertEquals(payloadStart, "[  {    \"id\": 13973,    \"category\": {   ...(truncated)");
	}
}
