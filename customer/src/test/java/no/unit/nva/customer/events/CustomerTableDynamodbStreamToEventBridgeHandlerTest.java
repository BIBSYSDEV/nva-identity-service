package no.unit.nva.customer.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

public class CustomerTableDynamodbStreamToEventBridgeHandlerTest {

    @Test
    void shouldHandleEmptyBatch() {
        var handler = new CustomerTableDynamodbStreamToEventBridgeHandler();

        var event = new DynamodbEvent();
        event.setRecords(Collections.emptyList());

        assertDoesNotThrow(() -> handler.handleRequest(event, new FakeContext()));
    }

    @Test
    void shouldProcessRecords() {
        var handler = new CustomerTableDynamodbStreamToEventBridgeHandler();

        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        var identifier = UUID.randomUUID();
        streamRecord.setNewImage(getAttributeMap(identifier));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        assertDoesNotThrow(() -> handler.handleRequest(event, new FakeContext()));
    }

    private Map<String, AttributeValue> getAttributeMap(UUID identifier) {
        return Map.of("identifier", new AttributeValue().withS(identifier.toString()),
                      "rightsRetentionStrategy", new AttributeValue().withM(Map.of("type",
                                                                                   new AttributeValue().withS(
                                                                                       "NullRightsRetentionStrategy"))));
    }
}
