package no.unit.nva.customer.get.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimWithClaimer;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ClaimedChannel")
public record ChannelClaimResponse(CustomerResponse claimedBy, ChannelClaimDto channelClaim) {

    private static final String API_HOST = "API_HOST";
    private static final String CUSTOMER = "customer";
    private static final String ID_FIELD = "id";
    private static final String CHANNEL_CLAIM = "channel-claim";

    @JsonIgnore
    public static ChannelClaimResponse create(ChannelClaimWithClaimer channelClaim) {
        return new ChannelClaimResponse(new CustomerResponse(channelClaim.customerId(), channelClaim.cristinId()),
                                        channelClaim.channelClaim());
    }

    @JsonProperty(ID_FIELD)
    public URI getId() {
        var identifier = UriWrapper.fromUri(channelClaim().channel()).getLastPathElement();
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(identifier)
                   .getUri();
    }
}
