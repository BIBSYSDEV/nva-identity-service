package no.unit.nva.customer.create;

import java.util.List;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import no.unit.nva.customer.model.PublicationInstanceTypes;

public record ChannelConstraintRequest(ChannelConstraintPolicy publishingPolicy,
                                       ChannelConstraintPolicy editingPolicy,
                                       List<PublicationInstanceTypes> scope) {

    public ChannelConstraintDto toDto() {
        return new ChannelConstraintDto(publishingPolicy(), editingPolicy(), scope());
    }
}
