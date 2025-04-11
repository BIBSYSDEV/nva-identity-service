package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.UUID;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI cristinId) {

    @JsonIgnore
    public boolean isClaimedBy(URI cristinId) {
        return cristinId.equals(cristinId());
    }

    public UUID channelClaimIdentifier() {
        return channelClaim().identifier();
    }
}
