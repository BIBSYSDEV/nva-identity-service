package no.unit.nva.customer.get.response;

import static java.util.stream.Collectors.collectingAndThen;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.CustomerDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ChannelClaims")
public record ChannelClaimsListResponse(Collection<ChannelClaimResponse> channelClaims) {

    public static ChannelClaimsListResponse fromChannelClaims(
        Collection<SimpleEntry<ChannelClaimDto, CustomerDto>> channelClaims) {
        return channelClaims.stream()
                    .map(ChannelClaimsListResponse::getChannelClaimResponse)
                    .collect(collectingAndThen(
                        Collectors.toList(),
                        ChannelClaimsListResponse::new
                    ));
    }

    private static ChannelClaimResponse getChannelClaimResponse(SimpleEntry<ChannelClaimDto, CustomerDto> simpleEntry) {
        var claimedBy = new CustomerResponse(simpleEntry.getValue().getId(), simpleEntry.getValue().getCristinId());
        var channelClaimDto = simpleEntry.getKey();

        return new ChannelClaimResponse(
            claimedBy,
            channelClaimDto);
    }
}

