package no.unit.nva.customer;

import java.net.URI;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class ChannelClaimIdProducer {
    private static final String API_HOST = "API_HOST";
    private static final String CUSTOMER = "customer";
    private static final String CHANNEL_CLAIM = "channel-claim";

    private ChannelClaimIdProducer() {
    }

    public static URI channelClaimId(ChannelClaimDto claim) {
        var identifier = UriWrapper.fromUri(claim.channel()).getLastPathElement();
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(identifier)
                   .getUri();
    }
}
