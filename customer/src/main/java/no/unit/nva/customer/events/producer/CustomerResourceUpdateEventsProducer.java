package no.unit.nva.customer.events.producer;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import java.util.List;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;

@FunctionalInterface
public interface CustomerResourceUpdateEventsProducer {
    List<ResourceUpdateEvent<ChannelClaim>> produceEvents(DynamodbStreamRecord record);
}
