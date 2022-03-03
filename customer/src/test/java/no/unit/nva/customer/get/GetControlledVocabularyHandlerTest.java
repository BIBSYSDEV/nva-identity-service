package no.unit.nva.customer.get;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.interfaces.VocabularyList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import nva.commons.apigatewayv2.MediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetControlledVocabularyHandlerTest extends CustomerDynamoDBLocal {

    public static final Context CONTEXT = mock(Context.class);
    private GetControlledVocabularyHandler handler;
    private DynamoDBCustomerService customerService;
    private CustomerDto existingCustomer;

    @BeforeEach
    public void init() {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(dynamoClient);
        existingCustomer = attempt(CustomerDataGenerator::createSampleCustomerDto)
            .map(customerInput -> customerService.createCustomer(customerInput))
            .orElseThrow();
        handler = new GetControlledVocabularyHandler(customerService);
    }

    @Test
    public void handleRequestReturnsOkWhenARequestWithAnExistingIdentifierIsSubmitted() {
        var response = sendGetRequest(getExistingCustomerIdentifier());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsNotFoundWhenARequestWithANonExistingIdentifierIsSubmitted() {
        var response = sendGetRequest(randomCustomerIdentifier());
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void handleRequestReturnsOutputWithLinkedDataContextWhenRequestIsSubmitted() throws IOException {
        var response = sendGetRequest(getExistingCustomerIdentifier());
        var body = defaultRestObjectMapper.mapFrom(response.getBody());
        String contextValue = body.get(LINKED_DATA_CONTEXT).toString();
        assertThat(contextValue, is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }

    @Test
    void handleRequestReturnsListWithIdEqualToTheGetPathOfTheResource() throws IOException {
        var request =
            createRequestWithMediaType(existingCustomer.getIdentifier(), MediaTypes.APPLICATION_JSON_LD);
        var response = handler.handleRequest(request, CONTEXT);

        var body = defaultRestObjectMapper.mapFrom(response.getBody());
        String idValue = body.get(LINKED_DATA_ID).toString();
        URI expectedId = URI.create(existingCustomer.getId().toString() + "/vocabularies");
        assertThat(idValue, is(equalTo(expectedId.toString())));
    }

    @Test
     void handleRequestReturnsControlledVocabulariesOfSpecifiedCustomerWhenCustomerIdIsValid()
        throws IOException {
        var response = sendGetRequest(getExistingCustomerIdentifier());
        VocabularyList body = VocabularyList.fromJson(response.getBody());
        Set<VocabularyDto> actualVocabularySettings = body.getVocabularies();
        assertThat(actualVocabularySettings, is(equalTo(existingCustomer.getVocabularies())));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonLdWhenAcceptHeaderIsJsonLd() throws IOException {
        var response = sendGetRequest(getExistingCustomerIdentifier());
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    void handleRequestReturnsResponseWithContentTypeJsonWhenAcceptHeaderIsJson() throws IOException {
        var response = sendRequestAcceptingJson(getExistingCustomerIdentifier());
        String content = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        assertThat(content, is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    private <T> APIGatewayProxyResponseEvent sendGetRequest(UUID identifier) {
        var request = createRequestWithMediaType(identifier, MediaTypes.APPLICATION_JSON_LD);
        return handler.handleRequest(request, CONTEXT);
    }

    private UUID getExistingCustomerIdentifier() {
        return existingCustomer.getIdentifier();
    }

    private APIGatewayProxyResponseEvent sendRequestAcceptingJson(UUID identifier) {
        var request = createRequestWithMediaType(identifier, MediaType.JSON_UTF_8);
        return handler.handleRequest(request, CONTEXT);
    }

    private APIGatewayProxyRequestEvent createRequestWithMediaType(UUID identifier, MediaType acceptHeader) {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of("identifier", identifier.toString()))
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()));
    }

    private UUID randomCustomerIdentifier() {
        return UUID.randomUUID();
    }
}
