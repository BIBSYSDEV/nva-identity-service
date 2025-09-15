package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.useraccessservice.usercreation.person.IdentityServiceErrorCodes;
import org.zalando.problem.Status;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class IdentityServiceExceptionsTest {

    @Test
    void identityServiceNotFoundExceptionShouldReturn404StatusCode() {
        var exception = new IdentityServiceNotFoundException("Person not found");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-2001: Person not found")));
    }

    @Test
    void identityServiceNotFoundExceptionShouldHaveCorrectMessage() {
        var exception = new IdentityServiceNotFoundException("Person with id 123 not found");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-2001: Person with id 123 not found")));
    }

    @Test
    void identityServiceUpstreamBodyParsingExceptionShouldReturn502StatusCode() {
        var cause = new RuntimeException("Parse error");
        var exception = new IdentityServiceUpstreamBodyParsingException("Failed to parse", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-3002: Failed to parse")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceUpstreamBodyParsingExceptionShouldHandleNullCause() {
        var exception = new IdentityServiceUpstreamBodyParsingException("Failed to parse", null);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-3002: Failed to parse")));
    }

    @Test
    void identityServiceUpstreamInternalServerErrorExceptionShouldReturn502StatusCode() {
        var exception = new IdentityServiceUpstreamInternalServerErrorException("Upstream error");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-3001: Upstream error")));
    }

    @Test
    void identityServiceUpstreamInternalServerErrorExceptionShouldAcceptCause() {
        var cause = new RuntimeException("Network error");
        var exception = new IdentityServiceUpstreamInternalServerErrorException("Upstream error", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-3001: Upstream error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceMissingRequiredFieldsExceptionShouldReturn400StatusCode() {
        var exception = new IdentityServiceMissingRequiredFieldsException(
                "Missing fields: id=null, firstname=John, surname=null");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), containsString("Missing fields"));
    }

    @Test
    void identityServiceMissingRequiredFieldsExceptionShouldFormatMessage() {
        var exception = new IdentityServiceMissingRequiredFieldsException("Missing required fields");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), containsString("Missing required fields"));
    }

    @Test
    void identityServiceCreateFailedExceptionShouldReturn502StatusCode() {
        var cause = new RuntimeException("Create failed");
        var exception = new IdentityServiceCreateFailedException("Failed to create person", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-3003: Failed to create person")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceExceptionBaseClassShouldAcceptCause() {
        var cause = new RuntimeException("Original error");
        var exception = new IdentityServiceException(IdentityServiceErrorCodes.GENERIC_ERROR, "Generic error", cause);
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1000: Generic error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceAlreadyExistsExceptionShouldReturn409StatusCode() {
        var exception = new IdentityServiceAlreadyExistsException("Person already exists");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-2002: Person already exists")));
    }

    @Test
    void identityServiceAlreadyExistsExceptionShouldAcceptCause() {
        var cause = new RuntimeException("Already exists error");
        var exception = new IdentityServiceAlreadyExistsException("Person already exists", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-2002: Person already exists")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceUnavailableExceptionShouldReturn504StatusCode() {
        var exception = new IdentityServiceUnavailableException("Service unavailable");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1002: Service unavailable")));
    }

    @Test
    void identityServiceUnavailableExceptionShouldAcceptCause() {
        var cause = new IOException("Connection timeout");
        var exception = new IdentityServiceUnavailableException("Service unavailable", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1002: Service unavailable")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceUnavailableExceptionShouldAcceptOnlyCause() {
        var cause = new IOException("Connection refused");
        var exception = new IdentityServiceUnavailableException(cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), containsString("unavailable"));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void identityServiceUnavailableExceptionWithDetailsShouldFormatMessage() {
        var cause = new IOException("Connection timeout");
        var exception = IdentityServiceUnavailableException.withDetails("https://api.cristin.no/persons/123", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), containsString("Unable to connect"));
        assertThat(exception.getMessage(), containsString("https://api.cristin.no/persons/123"));
        assertThat(exception.getMessage(), containsString("Connection timeout"));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
    
    @Test
    void identityServiceErrorCodesFormatMessageShouldReturnProblemJson() throws Exception {
        var formatted = IdentityServiceErrorCodes.formatMessage("1001", "Test message");
        
        // Parse the JSON to verify it's valid Problem format
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(formatted);
        
        assertNotNull(json.get("type"));
        assertThat(json.get("type").asText(), containsString("errors/1001"));
        assertEquals("IdentityService-1001: Test message", json.get("title").asText());
        assertEquals("Test message", json.get("detail").asText());
        assertEquals(400, json.get("status").asInt());
    }
    
    @Test
    void identityServiceErrorCodesCreateProblemShouldReturnProblemObject() {
        var problem = IdentityServiceErrorCodes.createProblem("2001", "Person not found");
        
        assertNotNull(problem);
        assertThat(problem.getType().toString(), containsString("errors/2001"));
        assertEquals("IdentityService-2001: Person not found", problem.getTitle());
        assertEquals("Person not found", problem.getDetail());
        assertEquals(Status.NOT_FOUND, problem.getStatus());
    }
    
    @Test
    void identityServiceErrorCodesFormatMessageWithStatusShouldUseProvidedStatus() throws Exception {
        var formatted = IdentityServiceErrorCodes.formatMessage("1001", "Test message", Status.FORBIDDEN);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(formatted);
        
        assertEquals(403, json.get("status").asInt());
    }
    
    @Test
    void identityServiceExceptionShouldSupportErrorCodes() {
        var exception = new IdentityServiceException("1001", "Test message");
        assertThat(exception.getErrorCode(), is(equalTo("1001")));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1001: Test message")));
    }
    
    @Test
    void identityServiceExceptionShouldSupportErrorCodesWithCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new IdentityServiceException("1001", "Test message", cause);
        assertThat(exception.getErrorCode(), is(equalTo("1001")));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1001: Test message")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
    
    @Test
    void identityServiceUnavailableExceptionShouldUseInfrastructureErrorCode() {
        var exception = new IdentityServiceUnavailableException("Connection failed");
        assertThat(exception.getErrorCode(), is(equalTo("1002")));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1002: Connection failed")));
    }
    
    @Test
    void identityServiceMissingNinExceptionShouldReturn400StatusCode() {
        var exception = new IdentityServiceMissingNinException();
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1003: Missing National Identity Number in user attributes")));
        assertThat(exception.getErrorCode(), is(equalTo("1003")));
    }
    
    @Test
    void identityServiceMissingNinExceptionShouldAcceptCustomMessage() {
        var exception = new IdentityServiceMissingNinException("Custom NIN missing message");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), is(equalTo("IdentityService-1003: Custom NIN missing message")));
        assertThat(exception.getErrorCode(), is(equalTo("1003")));
    }
    
    @Test
    void identityServiceMissingNinExceptionShouldUseCorrectErrorCode() {
        var exception = new IdentityServiceMissingNinException();
        assertThat(exception.getErrorCode(), is(equalTo(IdentityServiceErrorCodes.MISSING_NIN)));
        assertThat(IdentityServiceMissingNinException.HTTP_STATUS_CODE, is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
}