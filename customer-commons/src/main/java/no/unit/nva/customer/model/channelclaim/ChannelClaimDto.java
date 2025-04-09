package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ChannelClaim")
public record ChannelClaimDto(URI channelId, ChannelConstraintDto constraint) implements JsonSerializable {
    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }
}
