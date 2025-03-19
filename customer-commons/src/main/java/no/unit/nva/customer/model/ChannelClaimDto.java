package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.List;

public record ChannelClaimDto(URI channel, List<PublicationInstanceTypes> scope,
                              List<ChannelConstraint> constraints) implements JsonSerializable {
    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
