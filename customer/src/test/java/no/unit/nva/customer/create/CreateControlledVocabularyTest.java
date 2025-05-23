package no.unit.nva.customer.create;

import com.google.common.net.MediaType;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.testing.CreateUpdateControlledVocabularySettingsTests;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

class CreateControlledVocabularyTest extends CreateUpdateControlledVocabularySettingsTests {

    @BeforeEach
    public void init() throws ApiGatewayException {
        super.init();
    }

    @Override
    protected ControlledVocabularyHandler<?, ?> createHandler() {
        return new CreateControlledVocabularyHandler(customerService, new Environment());
    }

    @Override
    protected CustomerDto createExistingCustomer() {
        return CustomerDataGenerator.createSampleCustomerDto().copy()
            .withVocabularies(Collections.emptySet())
            .build();
    }

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
        throws IOException, NotFoundException {
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
    void handleRequestReturnsBadRequestWhenInputBodyIsNotValid() throws IOException {
        CustomerDto invalidBody = CustomerDataGenerator.createSampleCustomerDto();
        var request = createRequest(existingIdentifier(), invalidBody, MediaTypes.APPLICATION_JSON_LD);
        var response = sendRequest(handler, request, Problem.class);
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

    @Test
    void handleRequestReturnsCreatedVocabularyListWhenUserWithAccessRightManageOwnAffiliationsCreatesVocabulary()
        throws IOException {
        var result = sendRequestWithAccessRight(existingIdentifier(), AccessRight.MANAGE_OWN_AFFILIATION);
        assertThat(result.getResponse().getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }
}