package no.unit.nva.customer.get;

import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetControlledVocabularyHandlerTest extends CustomerDynamoDBLocal {

    public static final Context CONTEXT = mock(Context.class);
    private final Environment environment = new Environment();
    private ByteArrayOutputStream outputStream;
    private GetControlledVocabularyHandler handler;
    private DynamoDBCustomerService customerService;
    private CustomerDto existingCustomer;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        this.outputStream = new ByteArrayOutputStream();
        customerService = new DynamoDBCustomerService(ddb, ObjectMapperConfig.objectMapper, environment);
        existingCustomer = CustomerDataGenerator.crateSampleCustomerDto();
        attempt(() -> customerService.createCustomer(existingCustomer)).orElseThrow();
        handler = new GetControlledVocabularyHandler(customerService);
    }

    @Test
    public void handleRequestReturnsOkWhenARequestWithAnExistingIdentifierIsSubmitted() throws IOException {
        GatewayResponse<VocabularySettingsList> response = sendGetRequest(getExistingCustomerIdentifier());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsNotFoundWhenARequestWithANonExistingIdentifierIsSubmitted() throws IOException {
        GatewayResponse<VocabularySettingsList> response = sendGetRequest(randomCustomerIdentifier());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    public void handleRequestReturnsOutputWithLinkedDataContextWhenRequestIsSubmitted() throws IOException {
        GatewayResponse<ObjectNode> response = sendGetRequest(getExistingCustomerIdentifier());
        ObjectNode body = response.getBodyObject(ObjectNode.class);
        String contextValue = body.get(LINKED_DATA_CONTEXT).textValue();
        assertThat(contextValue, is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    public void handleRequestReturnsListWithIdEqualToTheNamespaceOfCustomers() throws IOException {
        InputStream request = createRequestWithMediaType(existingCustomer.getIdentifier(),
                                                         MediaTypes.APPLICATION_JSON_LD);
        handler.handleRequest(request, outputStream, CONTEXT);
        GatewayResponse<ObjectNode> response = GatewayResponse.fromOutputStream(outputStream);
        ObjectNode body = response.getBodyObject(ObjectNode.class);
        String idValue = body.get(LINKED_DATA_ID).textValue();
        assertThat(idValue, is(equalTo(ID_NAMESPACE.toString())));
    }

    @Test
    public void handleRequestReturnsControlledVocabulariesOfSpecifiedCustomerWhenCustomerIdIsValid()
        throws IOException {
        GatewayResponse<VocabularySettingsList> response = sendGetRequest(getExistingCustomerIdentifier());
        VocabularySettingsList body = response.getBodyObject(VocabularySettingsList.class);
        Set<VocabularySettingDto> actualVocabularySettings = body.getVocabularySettings();
        assertThat(actualVocabularySettings, is(equalTo(existingCustomer.getVocabularySettings())));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        GatewayResponse<VocabularySettingsList> response = sendGetRequest(getExistingCustomerIdentifier());
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    public void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        GatewayResponse<VocabularySettingsList> response =
            sendRequestAcceptingJson(getExistingCustomerIdentifier());
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    private <T> GatewayResponse<T> sendGetRequest(UUID identifier)
        throws IOException {
        InputStream request = createRequestWithMediaType(identifier, MediaTypes.APPLICATION_JSON_LD);
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream);
    }

    private UUID getExistingCustomerIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private <T> GatewayResponse<T> sendRequestAcceptingJson(UUID identifier)
        throws IOException {
        InputStream request = createRequestWithMediaType(identifier, MediaType.JSON_UTF_8);
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream);
    }

    private InputStream createRequestWithMediaType(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapperWithEmpty)
            .withPathParameters(Map.of("identifier", identifier.toString()))
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()))
            .build();
    }

    private UUID randomCustomerIdentifier() {
        return UUID.randomUUID();
    }
}
