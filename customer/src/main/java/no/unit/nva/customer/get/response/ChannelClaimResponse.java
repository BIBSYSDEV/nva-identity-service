package no.unit.nva.customer.get.response;

import no.unit.nva.customer.model.ChannelClaimDto;

public record ChannelClaimResponse(CustomerResponse claimedBy, ChannelClaimDto channelClaim) {

}
