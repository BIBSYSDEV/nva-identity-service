package no.unit.nva.customer.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.testing.CreateUpdateControlledVocabularySettingsTests;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import nva.commons.apigatewayv2.MediaTypes;
import org.junit.jupiter.api.Test;

class CreateControlledVocabularyTest extends CreateUpdateControlledVocabularySettingsTests {

    @Test
    void handleRequestReturnsCreatedWhenCreatingVocabularyForExistingCustomer() throws IOException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());
        assertThat(result.getResponse().getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }

    @Test
    void handleRequestReturnsCreatedVocabularyListWhenCreatingVocabularyForExistingCustomer()
        throws IOException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());

        VocabularyList actualBody = VocabularyList.fromJson(result.getResponse().getBody());
        assertThat(actualBody, is(equalTo(result.getExpectedBody())));
    }

    @Test
    void handleRequestSavesVocabularySettingsToDatabaseWhenCreatingSettingsForExistingCustomer()
        throws IOException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());
        var savedVocabularySettings =
            customerService.getCustomer(existingIdentifier()).getVocabularies();
        assertThat(savedVocabularySettings, is(equalTo(result.getExpectedBody().getVocabularies())));
    }

    @Test
    void handleRequestReturnsNotFoundWhenTryingToSaveSettingsForNonExistingCustomer()
        throws IOException {
        var result = sendRequestAcceptingJsonLd(UUID.randomUUID());
        assertThat(result.getResponse().getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void handleRequestReturnsBadRequestWhenInputBodyIsNotValid() {
        CustomerDto invalidBody = CustomerDataGenerator.createSampleCustomerDto();
        var request = createRequest(existingIdentifier(), invalidBody, MediaTypes.APPLICATION_JSON_LD);
        var response = handler.handleRequest(request, CONTEXT);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());
        assertThat(responseContentType(result.getResponse()), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        var result = sendRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        String content = responseContentType(result.getResponse());
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    void handleRequestReturnsUnsupportedTypeWhenAcceptedContentTypeIsNotSupported() throws IOException {
        var result = sendRequest(existingIdentifier(), MediaType.SOAP_XML_UTF_8);

        assertThat(result.getResponse().getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        String body = result.getResponse().getBody();
        for (MediaType mediaType : ControlledVocabularyHandler.SUPPORTED_MEDIA_TYPES) {
            assertThat(body, containsString(mediaType.toString()));
        }
    }

    @Test
    void handleRequestReturnsConflictWhenCustomerAlreadyHasVocabularySettings() throws IOException {
        assertThatExistingUserHasEmptyVocabularySettings();
        sendRequestAcceptingJsonLd(existingIdentifier());
        var result = sendRequestAcceptingJsonLd(existingIdentifier());

        assertThat(result.getResponse().getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }

    @Override
    protected ControlledVocabularyHandler<?, ?> createHandler() {
        return new CreateControlledVocabularyHandler(customerService);
    }

    @Override
    protected CustomerDto createExistingCustomer() {
        return CustomerDataGenerator.createSampleCustomerDto().copy()
            .withVocabularies(Collections.emptySet())
            .build();
    }
}