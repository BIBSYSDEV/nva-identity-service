package no.unit.nva.customer.create;

import java.net.URI;
import no.unit.nva.customer.model.ChannelClaimDto;

public record ChannelClaimRequest(URI channel, ChannelConstraintRequest channelConstraintRequest) {

    public static ChannelClaimRequest fromDto(ChannelClaimDto dto) {
        return new ChannelClaimRequest(dto.channel(), ChannelConstraintRequest.fromDto(dto.constraint()));
    }

    public ChannelClaimDto toDto() {
        return new ChannelClaimDto(channel(), channelConstraintRequest().toDto());
    }
}
