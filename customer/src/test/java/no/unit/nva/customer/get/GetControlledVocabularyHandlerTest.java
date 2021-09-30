package no.unit.nva.customer.get;

import static no.unit.nva.customer.model.LinkedDataContextUtils.ID_NAMESPACE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetControlledVocabularyHandlerTest {

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestReturnsOkWhenARequestIsSubmitted() throws IOException {
        InputStream request = createRequest();
        GetControlledVocabularyHandler handler = new GetControlledVocabularyHandler();
        handler.handleRequest(request, outputStream, mock(Context.class));
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsExpectedOutputWhenRequestIsSubmitted() throws IOException {
        InputStream request = createRequest();
        GetControlledVocabularyHandler handler = new GetControlledVocabularyHandler();
        handler.handleRequest(request, outputStream, mock(Context.class));
        GatewayResponse<VocabularySettingsList> response = GatewayResponse.fromOutputStream(outputStream);
        VocabularySettingsList body = response.getBodyObject(VocabularySettingsList.class);
        assertThat(body.getVocabularySettings(), contains(GetControlledVocabularyHandler.HARDCODED_RESPONSE));
    }

    @Test
    public void handleRequestReturnsOutputWithLinkedDataContextWhenRequestIsSubmitted() throws IOException {
        InputStream request = createRequest();
        GetControlledVocabularyHandler handler = new GetControlledVocabularyHandler();
        handler.handleRequest(request, outputStream, mock(Context.class));
        GatewayResponse<ObjectNode> response = GatewayResponse.fromOutputStream(outputStream);
        ObjectNode body = response.getBodyObject(ObjectNode.class);
        String contextValue = body.get(LINKED_DATA_CONTEXT).textValue();
        assertThat(contextValue, is(equalTo(LINKED_DATA_CONTEXT_VALUE.toString())));
    }
    @Test
    public void handleRequestReturnsListWithIdEqualToTheNamespaceOfCustomers() throws IOException {
        InputStream request = createRequest();
        GetControlledVocabularyHandler handler = new GetControlledVocabularyHandler();
        handler.handleRequest(request, outputStream, mock(Context.class));
        GatewayResponse<ObjectNode> response = GatewayResponse.fromOutputStream(outputStream);
        ObjectNode body = response.getBodyObject(ObjectNode.class);
        String idValue = body.get(LINKED_DATA_ID).textValue();
        assertThat(idValue, is(equalTo(ID_NAMESPACE.toString())));
    }

    private InputStream createRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapperWithEmpty)
            .withPathParameters(Map.of("customerId", randomCustomerId()))
            .build();
    }

    private String randomCustomerId() {
        return UUID.randomUUID().toString();
    }
}
