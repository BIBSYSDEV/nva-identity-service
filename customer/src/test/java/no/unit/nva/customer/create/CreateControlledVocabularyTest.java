package no.unit.nva.customer.create;

import static no.unit.nva.customer.ControlledVocabularyHandler.IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.customer.ObjectMapperConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateControlledVocabularyTest extends CustomerDynamoDBLocal {

    public static final Context CONTEXT = mock(Context.class);
    private CreateControlledVocabularyHandler handler;
    private CustomerDto existingCustomer;
    private ByteArrayOutputStream outputStream;
    private DynamoDBCustomerService customerService;

    @BeforeEach
    public void init() throws ApiGatewayException {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(this.ddb,
                                                      objectMapper,
                                                      new Environment());
        existingCustomer = customerService.createCustomer(CustomerDataGenerator.crateSampleCustomerDto());
        outputStream = new ByteArrayOutputStream();
        handler = new CreateControlledVocabularyHandler(customerService);
    }

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

    private VocabularySettingsList sendRequestAcceptingJsonLd(UUID uuid) throws IOException {
        return sendRequest(uuid, MediaTypes.APPLICATION_JSON_LD);
    }

    private VocabularySettingsList sendRequest(UUID uuid, MediaType acceptedContentType) throws IOException {
        VocabularySettingsList expectedBody = createRandomVocabularyList();
        InputStream request = addVocabularyForCustomer(uuid, expectedBody, acceptedContentType);
        handler.handleRequest(request, outputStream, CONTEXT);
        return expectedBody;
    }

    private String responseContentType(GatewayResponse<VocabularySettingsList> response) {
        return response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    }

    private UUID existingIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private VocabularySettingsList createRandomVocabularyList() {
        return new VocabularySettingsList(CustomerDataGenerator.randomVocabularyDtoSettings());
    }

    private <T> InputStream addVocabularyForCustomer(UUID customerIdentifer, T expectedBody,
                                                     MediaType acceptedMediaType)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(objectMapper)
            .withPathParameters(identifierToPathParameter(customerIdentifer))
            .withBody(expectedBody)
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptedMediaType.toString()))
            .build();
    }

    private Map<String, String> identifierToPathParameter(UUID identifier) {
        return Map.of(IDENTIFIER_PATH_PARAMETER, identifier.toString());
    }
}