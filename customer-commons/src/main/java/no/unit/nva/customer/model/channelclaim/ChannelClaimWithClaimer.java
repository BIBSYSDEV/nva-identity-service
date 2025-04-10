package no.unit.nva.customer.model.channelclaim;

import java.net.URI;
import java.util.UUID;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI cristinId) {

    public UUID channelClaimIdentifier() {
        return channelClaim.identifier();
    }
}
