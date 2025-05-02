package no.unit.nva.customer.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.Collections;
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

        event.setRecords(Collections.singletonList(new DynamodbEvent.DynamodbStreamRecord()));

        assertDoesNotThrow(() -> handler.handleRequest(event, new FakeContext()));
    }
}
