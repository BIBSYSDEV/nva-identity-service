package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI cristinId) {

    @JsonIgnore
    public boolean isClaimedBy(URI cristinId) {
        return cristinId.equals(cristinId());
    }

}
