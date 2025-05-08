package no.unit.nva.customer.events.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.Collection;
import java.util.List;
import no.unit.nva.customer.events.aws.JacksonAttributeValueConverter;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.events.producer.CustomerResourceUpdateEventsProducer;
import no.unit.nva.customer.events.producer.DefaultCustomerResourceUpdateEventsProducer;
import nva.commons.core.JacocoGenerated;

public class CustomerTableDynamodbStreamToEventBridgeHandler
    implements RequestHandler<DynamodbEvent, List<ResourceUpdateEvent<ChannelClaim>>> {

    private final CustomerResourceUpdateEventsProducer customerResourceUpdateEventsProducer;

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
        var attributeValueConverter = new JacksonAttributeValueConverter();
        this.customerResourceUpdateEventsProducer = new DefaultCustomerResourceUpdateEventsProducer(
            attributeValueConverter);
    }

    @Override
    public List<ResourceUpdateEvent<ChannelClaim>> handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        return dynamodbEvent.getRecords().stream()
                   .map(customerResourceUpdateEventsProducer::produceEvents)
                   .flatMap(Collection::stream)
                   .toList();
    }
}
