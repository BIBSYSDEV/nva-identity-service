package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static no.unit.nva.useraccess.events.EventsConfig.objectMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import no.unit.nva.events.models.ScanDatabaseRequest;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;

public class StartBatchScan implements RequestStreamHandler {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    public static final String DETAIL_TYPE = "topicInDetailType";
    private  final EventBridgeClient eventClient;

    @JacocoGenerated
    public StartBatchScan() {
        this(EventBridgeClient.create());
    }

    public StartBatchScan(EventBridgeClient eventClient) {
        this.eventClient = eventClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        ScanDatabaseRequest request = objectMapper.readValue(input, ScanDatabaseRequest.class);
        ScanDatabaseRequest newEvent = createEventAsExpectedByEventListener(request);
        eventClient.putEvents(createEvent(context, request));
        writeOutput(output, newEvent);
    }

    private PutEventsRequest createEvent(Context context, ScanDatabaseRequest request) {
        return PutEventsRequest.builder().entries(request.createNewEventEntry(EventsConfig.EVENT_BUS,
                                                                              DETAIL_TYPE,
                                                                              context.getInvokedFunctionArn())).build();
    }

    private void writeOutput(OutputStream output, ScanDatabaseRequest newEvent) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))) {
            writer.write(newEvent.toJsonString());
        }
    }

    private ScanDatabaseRequest createEventAsExpectedByEventListener(ScanDatabaseRequest input) {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                       input.getPageSize(),
                                       START_FROM_BEGINNING);
    }
}
