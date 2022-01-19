package no.unit.nva.useraccess.events;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.ScanDatabaseRequest;
import nva.commons.core.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBasedScanHandler
    extends DestinationsEventBridgeEventHandler<ScanDatabaseRequest, ScanDatabaseRequest> {

    private static final Logger logger = LoggerFactory.getLogger(EventBasedScanHandler.class);

    protected EventBasedScanHandler() {
        super(ScanDatabaseRequest.class);
    }

    @Override
    protected ScanDatabaseRequest processInputPayload(
        ScanDatabaseRequest input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<ScanDatabaseRequest>> event,
        Context context) {

        logger.info("Event:" + attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(event)).orElseThrow());
        return notImportant();
    }

    private ScanDatabaseRequest notImportant() {
        return null;
    }
}
