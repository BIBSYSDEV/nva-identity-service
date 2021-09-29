package no.unit.nva.customer.get;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.customer.model.VocabularySettingDto;
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
        GatewayResponse<VocabularySettingDto[]> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsExpectedOutputWhenRequestIsSubmitted() throws IOException {
        InputStream request = createRequest();
        GetControlledVocabularyHandler handler = new GetControlledVocabularyHandler();
        handler.handleRequest(request, outputStream, mock(Context.class));
        GatewayResponse<VocabularySettingDto[]> response = GatewayResponse.fromOutputStream(outputStream);
        List<VocabularySettingDto> body = Arrays.asList(response.getBodyObject(VocabularySettingDto[].class));
        assertThat(body, contains(GetControlledVocabularyHandler.HARDCODED_RESPONSE));
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
