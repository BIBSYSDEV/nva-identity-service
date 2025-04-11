package no.unit.nva.customer.get.response;

import static java.util.stream.Collectors.collectingAndThen;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.channelclaim.ChannelClaimWithClaimer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ChannelClaims")
public record ChannelClaimsListResponse(Collection<ChannelClaimResponse> channelClaims) {

    public static ChannelClaimsListResponse fromChannelClaims(Collection<ChannelClaimWithClaimer> channelClaims) {
        return channelClaims.stream()
                    .map(ChannelClaimsListResponse::getChannelClaimResponse)
                    .collect(collectingAndThen(
                        Collectors.toList(),
                        ChannelClaimsListResponse::new
                    ));
    }

    private static ChannelClaimResponse getChannelClaimResponse(ChannelClaimWithClaimer channelClaimWithClaimer) {
        var claimedBy = new CustomerResponse(channelClaimWithClaimer.customerId(), channelClaimWithClaimer.organizationId());
        return new ChannelClaimResponse(claimedBy, channelClaimWithClaimer.channelClaim());
    }
}

