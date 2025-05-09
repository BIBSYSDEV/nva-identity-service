package no.unit.nva.customer.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public interface IdentifiedResource {
    @JsonProperty("id")
    URI resourceId();
}
