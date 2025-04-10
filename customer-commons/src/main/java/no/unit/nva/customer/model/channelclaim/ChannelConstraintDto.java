package no.unit.nva.customer.model.channelclaim;

import no.unit.nva.commons.json.JsonSerializable;

import java.util.List;
import no.unit.nva.customer.model.PublicationInstanceTypes;

public record ChannelConstraintDto(ChannelConstraintPolicy publishingPolicy,
                                   ChannelConstraintPolicy editingPolicy,
                                   List<PublicationInstanceTypes> scope) implements JsonSerializable {
}
