package no.unit.nva.customer.events.emitter;

import java.util.List;
import no.unit.nva.customer.events.model.IdentifiedResource;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;

@FunctionalInterface
public interface ResourceUpdatedEventEmitter {
    <T extends IdentifiedResource> void emitEvents(List<ResourceUpdateEvent<T>> events, String detailType);
}
