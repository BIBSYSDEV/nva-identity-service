package no.unit.nva.customer.testing;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.get.GetControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import nva.commons.core.Environment;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.ControlledVocabularyHandler.IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;

public abstract class CreateUpdateControlledVocabularySettingsTests extends LocalCustomerServiceDatabase {

    public static final Context CONTEXT = new FakeContext();
    protected ControlledVocabularyHandler<?, ?> handler;
    protected CustomerDto existingCustomer;
    protected DynamoDBCustomerService customerService;
    protected ByteArrayOutputStream output;

    public void init() throws ApiGatewayException {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        existingCustomer = customerService.createCustomer(createExistingCustomer());
        handler = createHandler();
        output = new ByteArrayOutputStream();
    }

    protected abstract ControlledVocabularyHandler<?, ?> createHandler();

    protected abstract CustomerDto createExistingCustomer();

    protected ExpectedBodyActualResponseTuple sendRequestAcceptingJsonLd(UUID uuid) throws IOException {
        return sendRequest(uuid, MediaTypes.APPLICATION_JSON_LD);
    }

    protected ExpectedBodyActualResponseTuple sendRequest(UUID uuid, MediaType acceptedContentType)
        throws IOException {
        VocabularyList expectedBody = createRandomVocabularyList();
        var request = createRequest(uuid, expectedBody, acceptedContentType);
        output = new ByteArrayOutputStream();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, VocabularyList.class);
        return new ExpectedBodyActualResponseTuple(expectedBody, response);
    }

    protected VocabularyList createRandomVocabularyList() {
        return VocabularyList.fromCustomerDto(CustomerDataGenerator.createSampleCustomerDto());
    }

    protected <T> InputStream createRequest(UUID customerIdentifier,
                                            T expectedBody,
                                            MediaType acceptedMediaType)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
            .withPathParameters(identifierToPathParameter(customerIdentifier))
            .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
            .withBody(expectedBody)
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptedMediaType.toString()))
            .build();
    }

    protected Map<String, String> identifierToPathParameter(UUID identifier) {
        return Map.of(IDENTIFIER_PATH_PARAMETER, identifier.toString());
    }

    protected ExpectedBodyActualResponseTuple sendRequestWithAccessRight(UUID uuid, AccessRight accessRight)
        throws IOException {
        VocabularyList expectedBody = createRandomVocabularyList();
        var request = createRequest(uuid, expectedBody, accessRight);
        output = new ByteArrayOutputStream();
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, VocabularyList.class);
        return new ExpectedBodyActualResponseTuple(expectedBody, response);
    }

    protected <T> InputStream createRequest(UUID customerIdentifier, T expectedBody, AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(dtoObjectMapper)
            .withPathParameters(identifierToPathParameter(customerIdentifier))
            .withAccessRights(randomUri(), accessRight)
            .withBody(expectedBody)
            .build();
    }

    protected String responseContentType(GatewayResponse<?> response) {
        return response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    }

    protected void assertThatExistingUserHasEmptyVocabularySettings() throws IOException {
        GetControlledVocabularyHandler getHandler = new GetControlledVocabularyHandler(customerService,
                                                                                       new Environment());
        var getRequest = createGetRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        var response = sendRequest(getHandler, getRequest, VocabularyList.class);
        var getResponseObject = VocabularyList.fromJson(response.getBody());
        assertThat(getResponseObject.getVocabularies(), is(empty()));
    }

    protected <T> GatewayResponse<T> sendRequest(ControlledVocabularyHandler<?, ?> getHandler,
                                                 InputStream getRequest,
                                                 Class<T> responseType)
        throws IOException {
        getHandler.handleRequest(getRequest, output, CONTEXT);
        return GatewayResponse.fromOutputStream(output, responseType);
    }

    protected UUID existingIdentifier() {
        return existingCustomer.getIdentifier();
    }

    protected InputStream createGetRequest(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withPathParameters(Map.of("identifier", identifier.toString()))
            .withAccessRights(randomUri(), MANAGE_CUSTOMERS)
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()))
            .build();
    }

    public static class ExpectedBodyActualResponseTuple {

        private final VocabularyList expectedBody;
        private final GatewayResponse<VocabularyList> response;

        public ExpectedBodyActualResponseTuple(
            VocabularyList expectedBody, GatewayResponse<VocabularyList> response) {

            this.expectedBody = expectedBody;
            this.response = response;
        }

        public VocabularyList getExpectedBody() {
            return expectedBody;
        }

        public GatewayResponse<VocabularyList> getResponse() {
            return response;
        }
    }
}
