package no.unit.nva.customer.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.testing.CreateUpdateControlledVocabularySettingsTests;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Test;

public class CreateControlledVocabularyTest extends CreateUpdateControlledVocabularySettingsTests {

    @Test
    public void handleRequestReturnsCreatedWhenCreatingVocabularyForExistingCustomer() throws IOException {
        sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
    }

    @Test
    public void handleRequestReturnsCreatedVocabularyListWhenCreatingVocabularyForExistingCustomer()
        throws IOException {
        VocabularySettingsList expectedBody = sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        VocabularySettingsList body = response.getBodyObject(VocabularySettingsList.class);
        assertThat(body, is(equalTo(expectedBody)));
    }

    @Test
    public void handleRequestSavesVocabularySettingsToDatabaseWhenCreatingSettingsForExistingCustomer()
        throws IOException, ApiGatewayException {
        VocabularySettingsList expectedBody = sendRequestAcceptingJsonLd(existingIdentifier());
        Set<VocabularySettingDto> savedVocabularySettings =
            customerService.getCustomer(existingIdentifier()).getVocabularySettings();
        assertThat(savedVocabularySettings, is(equalTo(expectedBody.getVocabularySettings())));
    }

    @Test
    public void handleRequestReturnsNotFoundWhenTryingToSaveSettingsForNonExistingCustomer()
        throws IOException {
        sendRequestAcceptingJsonLd(UUID.randomUUID());
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenInputBodyIsNotValid()
        throws IOException {
        CustomerDto invalidBody = CustomerDataGenerator.crateSampleCustomerDto();
        InputStream request = addVocabularyForCustomer(existingIdentifier(), invalidBody,
                                                       MediaTypes.APPLICATION_JSON_LD);
        handler.handleRequest(request, outputStream, CONTEXT);
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(responseContentType(response), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        sendRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        String content = responseContentType(response);
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    public void handleRequestReturnsUnsupportedTypeWhenAcceptedContentTypeIsNotSupported() throws IOException {
        sendRequest(existingIdentifier(), MediaType.SOAP_XML_UTF_8);
        GatewayResponse<JsonNode> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        String body = response.getBodyObject(JsonNode.class).toString();
        for (MediaType mediaType : ControlledVocabularyHandler.SUPPORTED_MEDIA_TYPES) {
            assertThat(body, containsString(mediaType.toString()));
        }
    }

    @Test
    public void handleRequestReturnsConflictWhenCustomerAlreadyHasVocabularySettings() throws IOException {
        assertThatExistingUserHasEmptyVocabularySettings();
        outputStream = new ByteArrayOutputStream();
        sendRequestAcceptingJsonLd(existingIdentifier());
        outputStream = new ByteArrayOutputStream();
        sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CONFLICT)));
    }

    @Override
    protected ControlledVocabularyHandler<?, ?> createHandler() {
        return new CreateControlledVocabularyHandler(customerService);
    }

    @Override
    protected CustomerDto createExistingCustomer() {
        return CustomerDataGenerator.crateSampleCustomerDto().copy()
            .withVocabularySettings(Collections.emptySet())
            .build();
    }
}