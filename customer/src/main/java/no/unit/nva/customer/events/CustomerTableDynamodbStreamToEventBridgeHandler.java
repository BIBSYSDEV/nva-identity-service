package no.unit.nva.customer.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerTableDynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTableDynamodbStreamToEventBridgeHandler.class);

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
    }

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        dynamodbEvent.getRecords().forEach(this::processRecord);
        return null;
    }

    private void processRecord(DynamodbStreamRecord record) {
        logger.info("Processing record: {}", record);
    }
}
