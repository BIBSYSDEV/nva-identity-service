package no.unit.nva.customer.create;

import java.net.URI;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

public record ChannelClaimRequest(URI channel, ChannelConstraintRequest constraint) {

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(channel(), constraint().toDto());
    }
}
