package no.unit.nva.customer.events.model;

import java.net.URI;
import java.util.Set;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;

public record ChannelClaim(URI id, URI channelId, URI customerId, URI organizationId, Constraints constraint)
    implements IdentifiedResource {

    public record Constraints(Set<PublicationInstanceTypes> scope,
                              ChannelConstraintPolicy publishingPolicy,
                              ChannelConstraintPolicy editingPolicy) {

    }
}