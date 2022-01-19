package no.unit.nva.useraccess.events;

import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import no.unit.nva.events.models.ScanDatabaseRequest;

public class StartBatchScan implements RequestHandler<ScanDatabaseRequest, ScanDatabaseRequest> {

    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;

    public StartBatchScan() {

    }

    @Override
    public ScanDatabaseRequest handleRequest(ScanDatabaseRequest possiblyIncompleteRequest, Context context) {
        return createEventAsExpectedByEventListener(possiblyIncompleteRequest);
    }

    private ScanDatabaseRequest createEventAsExpectedByEventListener(ScanDatabaseRequest input) {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC,
                                       input.getPageSize(),
                                       START_FROM_BEGINNING);
    }
}
