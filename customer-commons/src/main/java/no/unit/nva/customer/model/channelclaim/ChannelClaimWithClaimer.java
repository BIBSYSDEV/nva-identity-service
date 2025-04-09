package no.unit.nva.customer.model.channelclaim;

import java.net.URI;

public record ChannelClaimWithClaimer(ChannelClaimDto channelClaim, URI customerId, URI cristinId) {

}
