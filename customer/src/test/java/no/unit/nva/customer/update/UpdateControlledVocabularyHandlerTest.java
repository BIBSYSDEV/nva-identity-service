package no.unit.nva.customer.update;

import static no.unit.nva.customer.update.UpdateControlledVocabularyHandler.VOCABULARY_SETTINGS_NOT_DEFINED_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyListDto;
import no.unit.nva.customer.testing.CreateUpdateControlledVocabularySettingsTests;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class UpdateControlledVocabularyHandlerTest extends CreateUpdateControlledVocabularySettingsTests {

    public static final Context CONTEXT = mock(Context.class);

    @Test
    public void handleRequestReturnsAcceptedWhenUpdatingVocabularyForExistingCustomer() throws IOException {
        sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_ACCEPTED)));
    }

    @Test
    public void handleRequestReturnsUpdatedVocabularyListWhenUpdatingVocabularyForExistingCustomer()
        throws IOException {
        VocabularyListDto expectedBody = sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
        VocabularyListDto body = response.getBodyObject(VocabularyListDto.class);
        assertThat(body, is(equalTo(expectedBody)));
    }

    @Test
    public void handleRequestSavesVocabularySettingsToDatabaseWhenUpdatingSettingsForExistingCustomer()
        throws IOException, ApiGatewayException {
        VocabularyListDto expectedBody = sendRequestAcceptingJsonLd(existingIdentifier());
        Set<VocabularyDto> savedVocabularySettings =
            customerService.getCustomer(existingIdentifier()).toCustomerDto().getVocabularies();
        assertThat(savedVocabularySettings, is(equalTo(expectedBody.getVocabularies())));
    }

    @Test
    public void handleRequestReturnsNotFoundWhenTryingToSaveSettingsForNonExistingCustomer()
        throws IOException {
        sendRequestAcceptingJsonLd(UUID.randomUUID());
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    public void handleRequestReturnsBadRequestWhenInputBodyIsNotValid()
        throws IOException {
        CustomerDto invalidBody = CustomerDataGenerator.createSampleCustomerDto();
        InputStream request = addVocabularyForCustomer(existingIdentifier(), invalidBody,
                                                       MediaTypes.APPLICATION_JSON_LD);
        handler.handleRequest(request, outputStream, CONTEXT);
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        sendRequestAcceptingJsonLd(existingIdentifier());
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(responseContentType(response), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        sendRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        GatewayResponse<VocabularyListDto> response = GatewayResponse.fromOutputStream(outputStream);
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
    public void handleRequestReturnsConflictWhenCustomerAlreadyHasVocabularySettings()
        throws IOException, ApiGatewayException {
        CustomerDao customerWithoutVocabularySettings = CustomerDataGenerator
            .createSampleCustomerDao();
        customerWithoutVocabularySettings.setVocabularies(Collections.emptySet());

        customerService.createCustomer(customerWithoutVocabularySettings);
        sendRequestAcceptingJsonLd(customerWithoutVocabularySettings.getIdentifier());
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
        String problemDetail = response.getBodyObject(Problem.class).getDetail();
        assertThat(problemDetail, containsString(VOCABULARY_SETTINGS_NOT_DEFINED_ERROR));
    }

    @Override
    protected ControlledVocabularyHandler<?, ?> createHandler() {
        return new UpdateControlledVocabularyHandler(customerService);
    }

    @Override
    protected CustomerDao createExistingCustomer() {
        return CustomerDataGenerator.createSampleCustomerDao();
    }
}
