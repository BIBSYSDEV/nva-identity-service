package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.EVENT_BUS;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static no.unit.nva.useraccess.events.EventsConfig.objectMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import no.unit.nva.events.models.ScanDatabaseRequest;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class StartBatchScan implements RequestStreamHandler {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    public static final String DETAIL_TYPE = "topicInDetailType";
    private static final Logger logger = LoggerFactory.getLogger(StartBatchScan.class);
    private final EventBridgeClient eventClient;

    @JacocoGenerated
    public StartBatchScan() {
        this(EventBridgeClient.create());
    }

    public StartBatchScan(EventBridgeClient eventClient) {
        this.eventClient = eventClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        var requestSentByUser = parseUserInput(input);
        var requestWithTopic = createEventAsExpectedByEventListener(requestSentByUser);
        emitEvent(context, requestWithTopic);
        logger.info("Emitted request:" + requestWithTopic.toJsonString());
    }

    private ScanDatabaseRequest parseUserInput(InputStream input) throws IOException {
        return objectMapper.readValue(input, ScanDatabaseRequest.class);
    }

    private ScanDatabaseRequest createEventAsExpectedByEventListener(ScanDatabaseRequest input) {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                       input.getPageSize(),
                                       START_FROM_BEGINNING);
    }

    private void emitEvent(Context context, ScanDatabaseRequest requestWithTopic) {
        eventClient.putEvents(createEvent(context, requestWithTopic));
    }

    private PutEventsRequest createEvent(Context context, ScanDatabaseRequest request) {
        return PutEventsRequest.builder().entries(createNewEventEntry(context, request)).build();
    }

    private PutEventsRequestEntry createNewEventEntry(Context context, ScanDatabaseRequest request) {
        return request.createNewEventEntry(EVENT_BUS,
                                           DETAIL_TYPE,
                                           context.getInvokedFunctionArn());
    }
}
