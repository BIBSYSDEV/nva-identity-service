package no.unit.nva.customer.create;

import java.net.URI;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

public record ChannelClaimRequest(URI channelId, ChannelConstraintRequest constraint) {

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(channelId(), constraint().toDto());
    }
}
