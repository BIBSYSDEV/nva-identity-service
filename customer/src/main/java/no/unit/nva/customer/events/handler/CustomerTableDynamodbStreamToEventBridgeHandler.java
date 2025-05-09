package no.unit.nva.customer.events.handler;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.Collection;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.events.aws.JacksonAttributeValueConverter;
import no.unit.nva.customer.events.emitter.EventBridgeClientResourceUpdatedEventEmitter;
import no.unit.nva.customer.events.emitter.ResourceUpdatedEventEmitter;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.events.producer.CustomerResourceUpdateEventsProducer;
import no.unit.nva.customer.events.producer.DefaultCustomerResourceUpdateEventsProducer;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

public class CustomerTableDynamodbStreamToEventBridgeHandler
    implements RequestHandler<DynamodbEvent, List<ResourceUpdateEvent<ChannelClaim>>> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTableDynamodbStreamToEventBridgeHandler.class);

    private final CustomerResourceUpdateEventsProducer customerResourceUpdateEventsProducer;
    private final ResourceUpdatedEventEmitter resourceUpdatedEventEmitter;

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
        this(EventBridgeClient.create());
    }

    public CustomerTableDynamodbStreamToEventBridgeHandler(EventBridgeClient eventBridgeClient) {
        this.customerResourceUpdateEventsProducer = new DefaultCustomerResourceUpdateEventsProducer(
            new JacksonAttributeValueConverter());
        this.resourceUpdatedEventEmitter = new EventBridgeClientResourceUpdatedEventEmitter(eventBridgeClient);
    }

    @Override
    public List<ResourceUpdateEvent<ChannelClaim>> handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        var events = dynamodbEvent.getRecords().stream()
                         .map(customerResourceUpdateEventsProducer::produceEvents)
                         .flatMap(Collection::stream)
                         .toList();

        var eventsAsJson = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(events)).orElseThrow();
        logger.info("Derived the following events from dynamodb stream: {}", eventsAsJson);

        resourceUpdatedEventEmitter.emitEvents(events);
        return events;
    }
}
