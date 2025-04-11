package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.UUID;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI organizationId) {

    @JsonIgnore
    public boolean isClaimedBy(URI cristinId) {
        return cristinId.equals(organizationId());
    }

    public UUID channelClaimIdentifier() {
        return channelClaim().identifier();
    }
}
