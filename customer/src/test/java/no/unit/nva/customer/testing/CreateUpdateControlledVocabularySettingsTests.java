package no.unit.nva.customer.testing;

import static no.unit.nva.customer.ControlledVocabularyHandler.IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.get.GetControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularyList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;

public abstract class CreateUpdateControlledVocabularySettingsTests extends CustomerDynamoDBLocal {

    public static final Context CONTEXT = mock(Context.class);
    protected ControlledVocabularyHandler<?, ?> handler;
    protected CustomerDto existingCustomer;
    protected ByteArrayOutputStream outputStream;
    protected DynamoDBCustomerService customerService;

    @BeforeEach
    public void init() throws ApiGatewayException {
        super.setupDatabase();
        customerService = new DynamoDBCustomerService(this.dynamoClient);
        existingCustomer = createExistingCustomer();
        customerService.createCustomer(existingCustomer);
        outputStream = new ByteArrayOutputStream();
        handler = createHandler();
    }

    protected abstract ControlledVocabularyHandler<?, ?> createHandler();

    protected abstract CustomerDto createExistingCustomer() throws ApiGatewayException;

    protected VocabularyList sendRequestAcceptingJsonLd(UUID uuid) throws IOException {
        return sendRequest(uuid, MediaTypes.APPLICATION_JSON_LD);
    }

    protected VocabularyList sendRequest(UUID uuid, MediaType acceptedContentType) throws IOException {
        VocabularyList expectedBody = createRandomVocabularyList();
        InputStream request = addVocabularyForCustomer(uuid, expectedBody, acceptedContentType);
        handler.handleRequest(request, outputStream, CONTEXT);
        return expectedBody;
    }

    protected String responseContentType(GatewayResponse<VocabularyList> response) {
        return response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    }

    protected UUID existingIdentifier() {
        return existingCustomer.getIdentifier();
    }

    protected VocabularyList createRandomVocabularyList() {
        return  VocabularyList.fromCustomerDto(CustomerDataGenerator.createSampleCustomerDto());
    }

    protected <T> InputStream addVocabularyForCustomer(UUID customerIdentifer, T expectedBody,
                                                       MediaType acceptedMediaType)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<T>(defaultRestObjectMapper)
            .withPathParameters(identifierToPathParameter(customerIdentifer))
            .withBody(expectedBody)
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptedMediaType.toString()))
            .build();
    }

    protected Map<String, String> identifierToPathParameter(UUID identifier) {
        return Map.of(IDENTIFIER_PATH_PARAMETER, identifier.toString());
    }

    protected InputStream createGetRequest(UUID identifier, MediaType acceptHeader)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(defaultRestObjectMapper)
            .withPathParameters(Map.of("identifier", identifier.toString()))
            .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader.toString()))
            .build();
    }

    protected void assertThatExistingUserHasEmptyVocabularySettings() throws IOException {
        GetControlledVocabularyHandler getHandler = new GetControlledVocabularyHandler(customerService);
        InputStream getRequest = createGetRequest(existingIdentifier(), MediaType.JSON_UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        getHandler.handleRequest(getRequest, outputStream, CONTEXT);
        GatewayResponse<VocabularyList> getResponse = GatewayResponse.fromOutputStream(outputStream);
        VocabularyList getResponseObject = getResponse.getBodyObject(VocabularyList.class);
        assertThat(getResponseObject.getVocabularies(), is(empty()));
    }
}
