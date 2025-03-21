package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;

import java.util.List;

public record ChannelConstraintDto(ChannelConstraintPolicy publishingPolicy,
                                   ChannelConstraintPolicy editingPolicy,
                                   List<PublicationInstanceTypes> scope) implements JsonSerializable {
}
