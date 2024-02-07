package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class GetControlledVocabularyHandlerTest extends LocalCustomerServiceDatabase {

    public static final Context CONTEXT = new FakeContext();
    private GetControlledVocabularyHandler handler;
    private DynamoDBCustomerService customerService;
    private CustomerDto existingCustomer;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(dynamoClient);
        existingCustomer = attempt(CustomerDataGenerator::createSampleCustomerDto)
            .map(customerInput -> customerService.createCustomer(customerInput))
            .orElseThrow();
        handler = new GetControlledVocabularyHandler(customerService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestReturnsOkWhenARequestWithAnExistingIdentifierIsSubmitted() throws IOException {
        var response = sendRequest(getExistingCustomerIdentifier(), VocabularyList.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void handleRequestReturnsNotFoundWhenARequestWithANonExistingIdentifierIsSubmitted() throws IOException {
        var response = sendRequest(randomCustomerIdentifier(), Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void handleRequestReturnsOutputWithLinkedDataContextWhenRequestIsSubmitted() throws IOException {
        var response = sendRequest(getExistingCustomerIdentifier(), VocabularyList.class);
        var body = JsonConfig.mapFrom(response.getBody());
        String contextValue = body.get(LINKED_DATA_CONTEXT).toString();
        assertThat(contextValue, is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    void handleRequestReturnsListWithIdEqualToTheGetPathOfTheResource() throws IOException {
        var request =
            createRequestWithMediaType(existingCustomer.getIdentifier(), MediaTypes.APPLICATION_JSON_LD);
        var response = sendRequest(request, VocabularyList.class);

        var body = JsonConfig.mapFrom(response.getBody());
        String idValue = body.get(LINKED_DATA_ID).toString();
        URI expectedId = URI.create(existingCustomer.getId().toString() + "/vocabularies");
        assertThat(idValue, is(equalTo(expectedId.toString())));
    }

    @Test
    void handleRequestReturnsControlledVocabulariesOfSpecifiedCustomerWhenCustomerIdIsValid()
        throws IOException {
        var response = sendRequest(getExistingCustomerIdentifier(), VocabularyList.class);
        VocabularyList body = VocabularyList.fromJson(response.getBody());
        var actualVocabularySettings = body.getVocabularies();
        assertThat(actualVocabularySettings, is(equalTo(existingCustomer.getVocabularies())));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        var response = sendRequest(getExistingCustomerIdentifier(), VocabularyList.class);
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        var response = sendRequestAcceptingJson(getExistingCustomerIdentifier(), VocabularyList.class);
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, CONTEXT);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private <T> GatewayResponse<T> sendRequest(UUID identifier, Class<T> responseType) throws IOException {
        var request = createRequestWithMediaType(identifier, MediaTypes.APPLICATION_JSON_LD);
        return sendRequest(request, responseType);
    }

    private UUID getExistingCustomerIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private <T> GatewayResponse<T> sendRequestAcceptingJson(UUID identifier, Class<T> responseType)
        throws IOException {
        var request = createRequestWithMediaType(identifier, MediaType.JSON_UTF_8);
        return sendRequest(request, responseType);
    }

    private InputStream createRequestWithMediaType(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
                   .withPathParameters(Map.of("identifier", identifier.toString()))
                   .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
                   .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()))
                   .build();
    }

    private UUID randomCustomerIdentifier() {
        return UUID.randomUUID();
    }
}
