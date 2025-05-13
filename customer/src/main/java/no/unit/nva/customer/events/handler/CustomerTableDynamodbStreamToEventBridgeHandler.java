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
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.events.producer.CustomerResourceUpdateEventsProducer;
import no.unit.nva.customer.events.producer.DefaultCustomerResourceUpdateEventsProducer;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

public class CustomerTableDynamodbStreamToEventBridgeHandler
    implements RequestHandler<DynamodbEvent, List<ResourceUpdateEvent<ChannelClaim>>> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTableDynamodbStreamToEventBridgeHandler.class);
    private static final String ENV_KEY_EVENT_BUS_NAME = "EVENT_BUS_NAME";
    private static final String DETAIL_TYPE_RESOURCE_UPDATE_CHANNEL_CLAIM = "nva.resourceupdate.channelclaim";

    private final CustomerResourceUpdateEventsProducer customerResourceUpdateEventsProducer;
    private final EventBridgeClient eventBridgeClient;
    private final Environment environment;

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
        this(new Environment(),
             defaultEventBridgeClient());
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
                   .httpClientBuilder(UrlConnectionHttpClient.builder())
                   .build();
    }

    public CustomerTableDynamodbStreamToEventBridgeHandler(final Environment environment,
                                                           final EventBridgeClient eventBridgeClient) {
        this.customerResourceUpdateEventsProducer = new DefaultCustomerResourceUpdateEventsProducer(
            new JacksonAttributeValueConverter());
        this.eventBridgeClient = eventBridgeClient;
        this.environment = environment;
    }

    @Override
    public List<ResourceUpdateEvent<ChannelClaim>> handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        var events = dynamodbEvent.getRecords().stream()
                         .map(customerResourceUpdateEventsProducer::produceEvents)
                         .flatMap(Collection::stream)
                         .toList();

        var eventsAsJson = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(events)).orElseThrow();
        logger.info("Derived the following events from dynamodb stream: {}", eventsAsJson);

        var eventEmitter = new EventBridgeClientResourceUpdatedEventEmitter(eventBridgeClient,
                                                                            environment.readEnv(ENV_KEY_EVENT_BUS_NAME),
                                                                            context);
        eventEmitter.emitEvents(events, DETAIL_TYPE_RESOURCE_UPDATE_CHANNEL_CLAIM);
        return events;
    }
}
