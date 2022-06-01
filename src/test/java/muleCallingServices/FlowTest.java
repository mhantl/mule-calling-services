package muleCallingServices;

import static org.mule.munit.common.mocking.Attribute.attribute;

import java.io.IOException;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.munit.runner.functional.FunctionalMunitSuite;
import org.mule.tck.MuleTestUtils;
import org.mule.util.IOUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.io.ClassPathResource;

public class FlowTest extends FunctionalMunitSuite {
	
	@Test
	public void testFlow() throws Exception {

		String requestBody = getFileContent("requests/inbound-request.xml");
		String responseBody = getFileContent("responses/outbound-response.json");

		whenMessageProcessor("request").ofNamespace("http")
		.withAttributes(attribute("name").ofNamespace("doc"))
		.thenReturn(createPayload(responseBody).getMessage());
		
		MuleMessage muleMessage = muleMessageWithPayload(requestBody);

		muleMessage.setProperty("http.request.uri",
				"/mc-xmlToJson2", PropertyScope.INBOUND);
		
		muleMessage.setProperty("host",
				"0.0.0.0:8082", PropertyScope.INBOUND);

		muleMessage.setProperty("http.method",
				"POST", PropertyScope.INBOUND);

		
		muleMessage.setProperty("http.status", 200, PropertyScope.INBOUND);
		
		MuleEvent result = runFlow("mc-xmlToJson2Flow", new DefaultMuleEvent(
				muleMessage, MessageExchangePattern.REQUEST_RESPONSE,
				MuleTestUtils.getTestFlow(muleContext)));
		
		
		verifyCallOfMessageProcessor("logger").ofNamespace("mule").times(2);
		System.out.println(result.getMessageAsString());
		 
		 String actual = "{id:123, name:\"John\"}";
		 JSONAssert.assertEquals(
		   "{id:123,name:\"John\"}", actual, JSONCompareMode.LENIENT);
		 
		 String actual2 = "{\"request\":{\"header\":{\"Content-Type\":\"application/json\",\"name\": \"userMMMM\",\"password\":\"123-456-789-0\"},\"customerId\":\"1111222333444\"}}";

		 JSONAssert.assertEquals(result.getMessageAsString(), actual2, JSONCompareMode.LENIENT);	 
	}	 
	
	@Override
	protected String getConfigResources() {
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.getControlDocumentBuilderFactory().setNamespaceAware(false);
		System.setProperty("environment", "local");
		return "mule-calling-services.xml";
	}

	private String getFileContent(String resource) throws IOException {
		return IOUtils.toString(new ClassPathResource("/" + resource)
				.getInputStream());
	}
	
	protected MuleEvent createPayload(String payload) throws Exception {
		MuleEvent testEvent = testEvent(payload);
		testEvent.getMessage().setProperty("http.status", new String("200"), PropertyScope.INBOUND);
		return testEvent;
	}
	
}