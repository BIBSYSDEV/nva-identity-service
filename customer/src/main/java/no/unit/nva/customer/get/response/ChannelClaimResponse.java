package no.unit.nva.customer.get.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.customer.ChannelClaimIdProducer;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimWithClaimer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ClaimedChannel")
public record ChannelClaimResponse(CustomerResponse claimedBy, ChannelClaimDto channelClaim) {

    private static final String ID_FIELD = "id";

    @JsonIgnore
    public static ChannelClaimResponse create(ChannelClaimWithClaimer channelClaim) {
        return new ChannelClaimResponse(new CustomerResponse(channelClaim.customerId(), channelClaim.cristinId()),
                                        channelClaim.channelClaim());
    }

    @JsonProperty(ID_FIELD)
    public URI getId() {
        return ChannelClaimIdProducer.channelClaimId(channelClaim());
    }
}
