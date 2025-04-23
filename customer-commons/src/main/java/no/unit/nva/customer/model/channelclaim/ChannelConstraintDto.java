package no.unit.nva.customer.model.channelclaim;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;

import java.util.List;
import no.unit.nva.customer.model.PublicationInstanceTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ChannelConstraint")
public record ChannelConstraintDto(ChannelConstraintPolicy publishingPolicy,
                                   ChannelConstraintPolicy editingPolicy,
                                   List<PublicationInstanceTypes> scope) implements JsonSerializable {
}
