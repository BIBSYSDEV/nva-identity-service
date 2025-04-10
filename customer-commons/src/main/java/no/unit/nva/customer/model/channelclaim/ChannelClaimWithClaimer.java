package no.unit.nva.customer.model.channelclaim;

import java.net.URI;
import nva.commons.core.JacocoGenerated;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI cristinId) {

    @JacocoGenerated
    public boolean isClaimedBy(URI cristinId) {
        return cristinId.equals(cristinId());
    }

}
