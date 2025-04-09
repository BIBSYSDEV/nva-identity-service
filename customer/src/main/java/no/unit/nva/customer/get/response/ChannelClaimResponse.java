package no.unit.nva.customer.get.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ClaimedChannel")
public record ChannelClaimResponse(CustomerResponse claimedBy, ChannelClaimDto channelClaim) {

}
