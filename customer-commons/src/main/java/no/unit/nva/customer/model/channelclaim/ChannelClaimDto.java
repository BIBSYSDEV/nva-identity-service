package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ChannelClaim")
public record ChannelClaimDto(URI channel, ChannelConstraintDto constraint) implements JsonSerializable {
    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }

    @JsonIgnore
    public UUID identifier() {
        var lastPathElement = UriWrapper.fromUri(channel()).getLastPathElement();
        return UUID.fromString(lastPathElement);
    }
}
