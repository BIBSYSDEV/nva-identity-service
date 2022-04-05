package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.EVENTS_CLIENT;
import static no.unit.nva.useraccess.events.EventsConfig.EVENT_BUS;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static no.unit.nva.useraccess.events.EventsConfig.SCAN_REQUEST_EVENTS_DETAIL_TYPE;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class StartBatchScan implements RequestStreamHandler {

    private static final Map<String, String> START_FROM_BEGINNING = null;

    private static final Logger logger = LoggerFactory.getLogger(StartBatchScan.class);
    private final EventBridgeClient eventClient;

    @JacocoGenerated
    public StartBatchScan() {
        this(EVENTS_CLIENT);
    }

    public StartBatchScan(EventBridgeClient eventClient) {
        this.eventClient = eventClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        var requestSentByUser = parseUserInput(input);
        var requestWithTopic = createEventAsExpectedByEventListener(requestSentByUser);
        emitEvent(context, requestWithTopic);
        logger.info("Emitted request {}", requestWithTopic);
    }

    private ScanDatabaseRequestV2 parseUserInput(InputStream input) throws IOException {
        String inputString = IoUtils.streamToString(input);
        return ScanDatabaseRequestV2.fromJson(inputString);
    }

    private ScanDatabaseRequestV2 createEventAsExpectedByEventListener(ScanDatabaseRequestV2 input) {
        return new ScanDatabaseRequestV2(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                         input.getPageSize(),
                                         START_FROM_BEGINNING);
    }

    private void emitEvent(Context context, ScanDatabaseRequestV2 requestWithTopic) {
        eventClient.putEvents(createEvent(context, requestWithTopic));
    }

    private PutEventsRequest createEvent(Context context, ScanDatabaseRequestV2 request) {
        return PutEventsRequest.builder().entries(createNewEventEntry(context, request)).build();
    }

    private PutEventsRequestEntry createNewEventEntry(Context context, ScanDatabaseRequestV2 request) {
        return request.createNewEventEntry(EVENT_BUS,SCAN_REQUEST_EVENTS_DETAIL_TYPE,context.getInvokedFunctionArn());
    }
}
