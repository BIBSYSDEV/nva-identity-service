package no.unit.nva.customer.get.response;

import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

public record ChannelClaimResponse(CustomerResponse claimedBy, ChannelClaimDto channelClaim) {

}
