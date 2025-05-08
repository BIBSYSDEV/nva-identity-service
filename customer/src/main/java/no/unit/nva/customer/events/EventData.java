package no.unit.nva.customer.events;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;

public record EventData(Action action, String resourceType, ChannelClaim data) {
    private static final String RESOURCE_TYPE_CHANNEL_CLAIM = "ChannelClaim";

    public static EventData addedChannelClaim(URI customerId, ChannelClaimDto claim) {
        return channelClaimEvent(customerId, Action.ADDED, claim);
    }

    public static EventData removedChannelClaim(URI customerId, ChannelClaimDto claim) {
        return channelClaimEvent(customerId, Action.REMOVED, claim);
    }

    public static EventData updatedChannelClaim(URI customerId, ChannelClaimDto claim) {
        return channelClaimEvent(customerId, Action.UPDATED, claim);
    }

    private static EventData channelClaimEvent(URI customerId, Action action, ChannelClaimDto claim) {
        var constraint =
            new Constraints(EnumSet.copyOf(claim.constraint().scope()),
                            claim.constraint().publishingPolicy(),
                            claim.constraint().editingPolicy());
        var channelClaim = new ChannelClaim(claim.channel(), customerId, constraint);
        return new EventData(action, RESOURCE_TYPE_CHANNEL_CLAIM, channelClaim);
    }

    enum Action {ADDED, REMOVED, UPDATED}

    record ChannelClaim(URI channelId, URI customerId, Constraints constraint) {

    }

    record Constraints(Set<PublicationInstanceTypes> scope,
                       ChannelConstraintPolicy publishingPolicy,
                       ChannelConstraintPolicy editingPolicy) {

    }
}
