package no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class PersonRegistryExceptionsTest {

    @Test
    void personRegistryNotFoundExceptionShouldReturn404StatusCode() {
        var exception = new PersonRegistryNotFoundException("Person not found");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-2001: Person not found")));
    }

    @Test
    void personRegistryNotFoundExceptionShouldHaveCorrectMessage() {
        var exception = new PersonRegistryNotFoundException("Person with id 123 not found");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-2001: Person with id 123 not found")));
    }

    @Test
    void personRegistryUpstreamBodyParsingExceptionShouldReturn502StatusCode() {
        var cause = new RuntimeException("Parse error");
        var exception = new PersonRegistryUpstreamBodyParsingException("Failed to parse", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-3002: Failed to parse")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryUpstreamBodyParsingExceptionShouldHandleNullCause() {
        var exception = new PersonRegistryUpstreamBodyParsingException("Failed to parse", null);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-3002: Failed to parse")));
    }

    @Test
    void personRegistryUpstreamInternalServerErrorExceptionShouldReturn502StatusCode() {
        var exception = new PersonRegistryUpstreamInternalServerErrorException("Upstream error");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-3001: Upstream error")));
    }

    @Test
    void personRegistryUpstreamInternalServerErrorExceptionShouldAcceptCause() {
        var cause = new RuntimeException("Network error");
        var exception = new PersonRegistryUpstreamInternalServerErrorException("Upstream error", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-3001: Upstream error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryMissingRequiredFieldsExceptionShouldReturn400StatusCode() {
        var exception = new PersonRegistryMissingRequiredFieldsException(
                "Missing fields: id=null, firstname=John, surname=null");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), containsString("Missing fields"));
    }

    @Test
    void personRegistryMissingRequiredFieldsExceptionShouldFormatMessage() {
        var exception = new PersonRegistryMissingRequiredFieldsException("Missing required fields");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(exception.getMessage(), containsString("Missing required fields"));
    }

    @Test
    void personRegistryCreateFailedExceptionShouldReturn502StatusCode() {
        var cause = new RuntimeException("Create failed");
        var exception = new PersonRegistryCreateFailedException("Failed to create person", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-3003: Failed to create person")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryExceptionBaseClassShouldWork() {
        var exception = new PersonRegistryException("Generic error");
        assertThat(exception.getMessage(), is(equalTo("Generic error")));
        assertThat(exception, is(notNullValue()));
    }

    @Test
    void personRegistryExceptionBaseClassShouldAcceptCause() {
        var cause = new RuntimeException("Original error");
        var exception = new PersonRegistryException(PersonRegistryErrorCodes.GENERIC_ERROR, "Generic error", cause);
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1000: Generic error")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryAlreadyExistsExceptionShouldReturn409StatusCode() {
        var exception = new PersonRegistryAlreadyExistsException("Person already exists");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-2002: Person already exists")));
    }

    @Test
    void personRegistryAlreadyExistsExceptionShouldAcceptCause() {
        var cause = new RuntimeException("Already exists error");
        var exception = new PersonRegistryAlreadyExistsException("Person already exists", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-2002: Person already exists")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryUnavailableExceptionShouldReturn504StatusCode() {
        var exception = new PersonRegistryUnavailableException("Service unavailable");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1002: Service unavailable")));
    }

    @Test
    void personRegistryUnavailableExceptionShouldAcceptCause() {
        var cause = new IOException("Connection timeout");
        var exception = new PersonRegistryUnavailableException("Service unavailable", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1002: Service unavailable")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryUnavailableExceptionShouldAcceptOnlyCause() {
        var cause = new IOException("Connection refused");
        var exception = new PersonRegistryUnavailableException(cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), containsString("unavailable"));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }

    @Test
    void personRegistryUnavailableExceptionWithDetailsShouldFormatMessage() {
        var cause = new IOException("Connection timeout");
        var exception = PersonRegistryUnavailableException.withDetails("https://api.cristin.no/persons/123", cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        assertThat(exception.getMessage(), containsString("Unable to connect"));
        assertThat(exception.getMessage(), containsString("https://api.cristin.no/persons/123"));
        assertThat(exception.getMessage(), containsString("Connection timeout"));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
    
    @Test
    void personRegistryErrorCodesFormatMessageShouldFormatCorrectly() {
        var formatted = PersonRegistryErrorCodes.formatMessage("1001", "Test message");
        assertThat(formatted, is(equalTo("PersonRegistry-1001: Test message")));
    }
    
    @Test
    void personRegistryExceptionShouldSupportErrorCodes() {
        var exception = new PersonRegistryException("1001", "Test message");
        assertThat(exception.getErrorCode(), is(equalTo("1001")));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1001: Test message")));
    }
    
    @Test
    void personRegistryExceptionShouldSupportErrorCodesWithCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new PersonRegistryException("1001", "Test message", cause);
        assertThat(exception.getErrorCode(), is(equalTo("1001")));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1001: Test message")));
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
    
    @Test
    void personRegistryExceptionShouldReturnNullErrorCodeForLegacyConstructors() {
        var exception = new PersonRegistryException("Legacy message");
        assertThat(exception.getErrorCode(), is(equalTo(null)));
        assertThat(exception.getMessage(), is(equalTo("Legacy message")));
    }
    
    @Test
    void personRegistryUnavailableExceptionShouldUseInfrastructureErrorCode() {
        var exception = new PersonRegistryUnavailableException("Connection failed");
        assertThat(exception.getErrorCode(), is(equalTo("1002")));
        assertThat(exception.getMessage(), is(equalTo("PersonRegistry-1002: Connection failed")));
    }
}