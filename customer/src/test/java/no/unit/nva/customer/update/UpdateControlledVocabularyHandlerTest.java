package no.unit.nva.customer.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.testing.CreateUpdateControlledVocabularySettingsTests;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.stubs.FakeContext;
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

import static no.unit.nva.customer.update.UpdateControlledVocabularyHandler.VOCABULARY_SETTINGS_NOT_DEFINED_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

public class UpdateControlledVocabularyHandlerTest extends CreateUpdateControlledVocabularySettingsTests {

    public static final Context CONTEXT = new FakeContext();

    @BeforeEach
    public void init() throws ApiGatewayException {
        super.init();
    }

    @Override
    protected ControlledVocabularyHandler<?, ?> createHandler() {
        return new UpdateControlledVocabularyHandler(customerService, new Environment());
    }

    @Override
    protected CustomerDto createExistingCustomer() {
        return CustomerDataGenerator.createSampleCustomerDto();
    }

    @Test
    public void handleRequestReturnsAcceptedWhenUpdatingVocabularyForExistingCustomer() throws IOException {
        var response = sendRequestAcceptingJsonLd(existingIdentifier()).getResponse();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    public void handleRequestReturnsUpdatedVocabularyListWhenUpdatingVocabularyForExistingCustomer()
        throws IOException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());
        var actualBody = VocabularyList.fromJson(result.getResponse().getBody());
        assertThat(actualBody, is(equalTo(result.getExpectedBody())));
    }

    @Test
    public void handleRequestSavesVocabularySettingsToDatabaseWhenUpdatingSettingsForExistingCustomer()
        throws IOException, NotFoundException {
        var result = sendRequestAcceptingJsonLd(existingIdentifier());
        var savedVocabularySettings = customerService.getCustomer(existingIdentifier()).getVocabularies();

        assertThat(savedVocabularySettings, is(equalTo(result.getExpectedBody().getVocabularies())));
    }

    @Test
    public void handleRequestReturnsNotFoundWhenTryingToSaveSettingsForNonExistingCustomer()
        throws IOException {
        var response = sendRequestAcceptingJsonLd(UUID.randomUUID()).getResponse();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenInputBodyIsNotValid()
        throws IOException {
        CustomerDto invalidBody = CustomerDataGenerator.createSampleCustomerDto();
        var request = createRequest(existingIdentifier(), invalidBody, MediaTypes.APPLICATION_JSON_LD);
        var response = sendRequest(handler, request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        var response = sendRequestAcceptingJsonLd(existingIdentifier()).getResponse();
        assertThat(responseContentType(response), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        var response = sendRequest(existingIdentifier(), MediaType.JSON_UTF_8).getResponse();
        String content = responseContentType(response);
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    public void handleRequestReturnsUnsupportedTypeWhenAcceptedContentTypeIsNotSupported() throws IOException {
        var response = sendRequest(existingIdentifier(), MediaType.SOAP_XML_UTF_8).getResponse();
        String body = response.getBody();
        for (MediaType mediaType : ControlledVocabularyHandler.SUPPORTED_MEDIA_TYPES) {
            assertThat(body, containsString(mediaType.toString()));
        }
    }

    @Test
    public void handleRequestReturnsBadRequestWhenCustomerAlreadyHasVocabularySettingsAndWeAttemptToEraseThem()
        throws IOException, ApiGatewayException {
        var customerWithoutVocabularySettings =
            customerService.createCustomer(createCustomerWithoutVocabularySettings());
        var response =
            sendRequestAcceptingJsonLd(customerWithoutVocabularySettings.getIdentifier()).getResponse();

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        String responseBody = response.getBody();
        assertThat(responseBody, containsString(VOCABULARY_SETTINGS_NOT_DEFINED_ERROR));
    }

    private CustomerDto createCustomerWithoutVocabularySettings() {
        return CustomerDataGenerator
            .createSampleCustomerDto().copy()
            .withVocabularies(Collections.emptySet())
            .build();
    }

    @Test
    public void handleRequestReturnsUpdatedVocabularyListWhenUserWithAccessRightManageOwnAffiliationsUpdatesVocabulary()
        throws IOException {
        var result = sendRequestWithAccessRight(existingIdentifier(), AccessRight.MANAGE_OWN_AFFILIATION);
        var actualBody = VocabularyList.fromJson(result.getResponse().getBody());
        assertThat(actualBody, is(equalTo(result.getExpectedBody())));
    }
}
