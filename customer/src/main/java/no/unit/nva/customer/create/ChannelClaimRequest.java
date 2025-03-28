package no.unit.nva.customer.create;

import java.net.URI;
import no.unit.nva.customer.model.ChannelClaimDto;

public record ChannelClaimRequest(URI channel, ChannelConstraintRequest channelConstraintRequest) {

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(channel(), channelConstraintRequest().toDto());
    }
}
