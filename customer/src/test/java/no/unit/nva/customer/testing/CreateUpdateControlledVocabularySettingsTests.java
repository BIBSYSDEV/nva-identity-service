package no.unit.nva.customer.testing;

import static no.unit.nva.customer.ControlledVocabularyHandler.IDENTIFIER_PATH_PARAMETER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.get.GetControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularyList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigatewayv2.MediaTypes;
import org.junit.jupiter.api.BeforeEach;

public abstract class CreateUpdateControlledVocabularySettingsTests extends CustomerDynamoDBLocal {

    public static final Context CONTEXT = mock(Context.class);
    protected ControlledVocabularyHandler<?, ?> handler;
    protected CustomerDto existingCustomer;
    protected DynamoDBCustomerService customerService;

    @BeforeEach
    public void init()  {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        existingCustomer = createExistingCustomer();
        customerService.createCustomer(existingCustomer);
        handler = createHandler();
    }

    protected abstract ControlledVocabularyHandler<?, ?> createHandler();

    protected abstract CustomerDto createExistingCustomer() ;

    protected ExpectedBodyActualResponseTuple sendRequestAcceptingJsonLd(UUID uuid) throws IOException {
        return sendRequest(uuid, MediaTypes.APPLICATION_JSON_LD);
    }

    protected ExpectedBodyActualResponseTuple sendRequest(UUID uuid, MediaType acceptedContentType) throws IOException {
        VocabularyList expectedBody = createRandomVocabularyList();
        var request = createRequest(uuid, expectedBody, acceptedContentType);
        var response=handler.handleRequest(request,  CONTEXT);
        return new ExpectedBodyActualResponseTuple(expectedBody,response);
    }

    protected String responseContentType(APIGatewayProxyResponseEvent response) {
        return response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    }

    protected UUID existingIdentifier() {
        return existingCustomer.getIdentifier();
    }

    protected VocabularyList createRandomVocabularyList() {
        return VocabularyList.fromCustomerDto(CustomerDataGenerator.createSampleCustomerDto());
    }

    protected <T> APIGatewayProxyRequestEvent createRequest(UUID customerIdentifier,
                                                            T expectedBody,
                                                            MediaType acceptedMediaType) {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(identifierToPathParameter(customerIdentifier))
            .withBody(expectedBody.toString())
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptedMediaType.toString()))
            .withMultiValueHeaders(Map.of(HttpHeaders.ACCEPT, List.of(acceptedMediaType.toString())));
    }

    protected Map<String, String> identifierToPathParameter(UUID identifier) {
        return Map.of(IDENTIFIER_PATH_PARAMETER, identifier.toString());
    }

    protected APIGatewayProxyRequestEvent createGetRequest(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of("identifier", identifier.toString()))
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()));
    }

    protected void assertThatExistingUserHasEmptyVocabularySettings() throws IOException {
        GetControlledVocabularyHandler getHandler = new GetControlledVocabularyHandler(customerService);
        var getRequest = createGetRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        var response =getHandler.handleRequest(getRequest, CONTEXT);
        var  getResponseObject =VocabularyList.fromJson(response.getBody());
        assertThat(getResponseObject.getVocabularies(), is(empty()));
    }

    public static class ExpectedBodyActualResponseTuple  {

        private final VocabularyList expectedBody;
        private final APIGatewayProxyResponseEvent response;

        public ExpectedBodyActualResponseTuple(
            VocabularyList expectedBody, APIGatewayProxyResponseEvent response) {

            this.expectedBody = expectedBody;
            this.response = response;
        }

        public VocabularyList getExpectedBody() {
            return expectedBody;
        }

        public APIGatewayProxyResponseEvent getResponse() {
            return response;
        }
    }
}
