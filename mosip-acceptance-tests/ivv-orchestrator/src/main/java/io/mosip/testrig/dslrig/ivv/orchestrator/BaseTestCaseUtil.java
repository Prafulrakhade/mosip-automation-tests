package io.mosip.testrig.dslrig.ivv.orchestrator;

import static io.restassured.RestAssured.given;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Reporter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.testrig.apirig.admin.fw.util.AdminTestUtil;
import io.mosip.testrig.apirig.admin.fw.util.TestCaseDTO;
import io.mosip.testrig.apirig.authentication.fw.precon.JsonPrecondtion;
import io.mosip.testrig.apirig.authentication.fw.util.RestClient;
import io.mosip.testrig.apirig.kernel.util.ConfigManager;
import io.mosip.testrig.apirig.service.BaseTestCase;
import io.mosip.testrig.dslrig.ivv.core.base.BaseStep;
import io.mosip.testrig.dslrig.ivv.core.dtos.Scenario;
import io.mosip.testrig.dslrig.ivv.e2e.constant.E2EConstants;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class BaseTestCaseUtil extends BaseStep {
	private static final Logger logger = LoggerFactory.getLogger(BaseTestCaseUtil.class);

	public static Properties props = new AdminTestUtil()
			.getproperty(TestRunner.getExternalResourcePath() + "/config/test-orchestrator_mz.properties");
	public static Properties propsKernel = new AdminTestUtil()
			.getproperty(TestRunner.getExternalResourcePath() + "config/Kernel.properties");
	public String baseUrl = ConfigManager.getpacketUtilityBaseUrl();

	public static final long DEFAULT_WAIT_TIME = 30000l;
	public static final long TIME_IN_MILLISEC = 1000l;

	public static PacketUtility packetUtility = new PacketUtility();
	public static Hashtable<String, Map<String, String>> hashtable = new Hashtable<>();

	// public static String scenario = null; // Neeed to check how to add in
	// scenario
	public static String partnerKeyUrl = null;
	public static String partnerId = null;
	public static HashMap<String, HashMap<String, String>> prereqDataSet = new HashMap<String, HashMap<String, String>>();

	public BaseTestCaseUtil() {
	}

	public String getDateTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMddHHmmssSSS");
		LocalDateTime now = LocalDateTime.now();
		return "DSL" + dtf.format(now);
	}

	public String getDateTimePrint() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-mmm-yyyy hh:mm:ss.s");
		LocalDateTime now = LocalDateTime.now();
		return "DSL Time: " + dtf.format(now);
	}

	public String encoder(String text) {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}

	public String readProperty() {
		String preRegistrationId = null;
		FileInputStream inputStrem = null;
		Properties props = new Properties();
		try {
			inputStrem = new FileInputStream(TestResources.getResourcePath() + "preReg/autoGeneratedId.properties");
			props.load(inputStrem);
			preRegistrationId = props.getProperty("CreatePrereg_All_Valid_Smoke_sid_preRegistrationId");
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			PacketUtility.closeInputStream(inputStrem);
		}
		return preRegistrationId;
	}

	public Object[] filterTestCases(Object[] testCases) {
		String testlable = BaseTestCase.testLevel;
		List<Object> filteredCases = new ArrayList<>();
		if (testlable.equalsIgnoreCase("smoke")) {
			for (Object object : testCases) {
				TestCaseDTO test = (TestCaseDTO) object;
				if (test.getTestCaseName().toLowerCase().contains(testlable.toLowerCase()))
					filteredCases.add(object);
			}
			return filteredCases.toArray();
		}
		return testCases;
	}

	public TestCaseDTO filterOutTestCase(Object[] testCases, String testLabel) {
		TestCaseDTO test = null;
		for (Object object : testCases) {
			test = (TestCaseDTO) object;
			if (test.getTestCaseName().toLowerCase().contains(testLabel.toLowerCase()))
				return test;
		}
		return test;
	}

	protected static String addContextToUrl(String url, Scenario.Step step) {

		String scenario = step.getScenario().getId() + ":" + step.getScenario().getDescription();
		String context = System.getProperty("env.user") + "_S" + scenario.substring(0, scenario.indexOf(':'))
				+ "_context";

		// String context=System.getProperty("env.user")+"_context";

		if (url.contains("?")) {
			String urlArr[] = url.split("\\?");
			return urlArr[0] + "/" + context + "?" + urlArr[1];
		} else if (url.contains("mockmv"))
			return url;
		else
			return url + "/" + context;
	}

	public static Response getRequest(String url, String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response getResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().get(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ getResponse.getBody().asString() + "</pre>");
		return getResponse;
	}

	public static Response getRequestWithQueryParam(String url, HashMap<String, String> contextKey, String opsToLog,
			Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response getResponse = given().relaxedHTTPSValidation().queryParams(contextKey).accept("*/*").log().all().when()
				.get(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ getResponse.getBody().asString() + "</pre>");
		return getResponse;
	}

	public Response postRequest(String url, String body, String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = RestClient.postRequest(url, body, MediaType.APPLICATION_JSON,
				MediaType.APPLICATION_JSON);
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}

	public Response putRequestWithBody(String url, String body, String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response puttResponse = given().relaxedHTTPSValidation().body(body).contentType(MediaType.APPLICATION_JSON)
				.accept("*/*").log().all().when().put(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ puttResponse.getBody().asString() + "</pre>");
		return puttResponse;
	}

	public Response putRequestWithBody(String url, String body, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + "" + " </b> <br/>" + body + "</pre>");
		Response puttResponse = given().relaxedHTTPSValidation().body(body).contentType(MediaType.APPLICATION_JSON)
				.accept("*/*").log().all().when().put(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ puttResponse.getBody().asString() + "</pre>");
		return puttResponse;
	}

	public Response putRequest(String url, String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response putResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().put(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ putResponse.getBody().asString() + "</pre>");
		return putResponse;
	}

	public Response deleteRequest(String url, String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response deleteResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().delete(url).then().log().all().extract()
				.response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ deleteResponse.getBody().asString() + "</pre>");
		return deleteResponse;
	}

	public Response deleteRequestWithoutStep(String url, String opsToLog) {
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response deleteResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().delete(url).then().log().all().extract()
				.response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ deleteResponse.getBody().asString() + "</pre>");
		return deleteResponse;
	}

	public Response deleteRequestWithQueryParam(String url, HashMap<String, String> map, String opsToLog,
			Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response deleteResponse = given().relaxedHTTPSValidation().queryParams(map).accept("*/*").log().all().when()
				.delete(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ deleteResponse.getBody().asString() + "</pre>");
		return deleteResponse;
	}

	public Response putRequestWithQueryParamAndBody(String url, String body, HashMap<String, String> map,
			String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = RestClient.putRequestWithQueryParamAndBody(url, body, map, MediaType.APPLICATION_JSON,
				"*/*");
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}

	public Response postRequestWithQueryParamAndBody(String url, String body, HashMap<String, String> map,
			String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = RestClient.postRequestWithQueryParamAndBody(url, body, map, MediaType.APPLICATION_JSON,
				MediaType.APPLICATION_JSON);
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}

	public Response postRequestWithPathParamAndBody(String url, String body, HashMap<String, String> map,
			String opsToLog, Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = given().contentType(ContentType.JSON).pathParams(map).body(body).log().all().when()
				.post(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}

	public Response postReqestWithCookiesAndBody(String url, String body, String token, String opsToLog,
			Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response posttResponse = given().relaxedHTTPSValidation().body(body).contentType(MediaType.APPLICATION_JSON)
				.accept("*/*").log().all().when().cookie("Authorization", token).post(url).then().log().all().extract()
				.response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ posttResponse.getBody().asString() + "</pre>");
		return posttResponse;
	}

	public Response putRequestWithQueryParam(String url, HashMap<String, String> map, String opsToLog,
			Scenario.Step step) {
		url = addContextToUrl(url, step);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response puttResponse = given().queryParams(map).relaxedHTTPSValidation().log().all().when().put(url).then()
				.log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ puttResponse.getBody().asString() + "</pre>");
		return puttResponse;
	}

	public void constantIntializer() {
		E2EConstants.MACHINE_ID = props.getProperty("machine_id");
		E2EConstants.CENTER_ID = props.getProperty("center_id");
		E2EConstants.USER_ID = props.getProperty("user_id");
		E2EConstants.USER_PASSWD = props.getProperty("user_passwd");
		E2EConstants.SUPERVISOR_ID = props.getProperty("supervisor_id");
		E2EConstants.PRECONFIGURED_OTP = props.getProperty("preconfigured_otp");
	}

	public static String getBioValueFromJson(String filePath) {
		String bioMetricData = null;
		try {
			String jsonObj = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
			bioMetricData = JsonPrecondtion.getValueFromJson(jsonObj, "response.(documents)[0].value");
		} catch (JsonParseException e) {
			logger.error(e.getMessage());
		} catch (JsonMappingException e) {
			logger.error(e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		return bioMetricData;
	}

}