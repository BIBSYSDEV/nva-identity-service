package no.unit.nva.customer.model.channelclaim;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

public record ChannelClaimDto(URI channel, ChannelConstraintDto constraint) implements JsonSerializable {
    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
