package no.unit.nva.customer.events.model;

import java.net.URI;
import java.util.EnumSet;
import no.unit.nva.customer.ChannelClaimIdProducer;
import no.unit.nva.customer.events.model.ChannelClaim.Constraints;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

public final class ChannelClaimUpdateEvents {

    private static final String RESOURCE_TYPE_CHANNEL_CLAIM = "ChannelClaim";

    private ChannelClaimUpdateEvents() {
    }

    public static ResourceUpdateEvent<ChannelClaim> addedChannelClaim(URI customerId, URI organizationId,
                                                                      ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, ResourceUpdateEvent.Action.ADDED, claim);
    }

    public static ResourceUpdateEvent<ChannelClaim> removedChannelClaim(URI customerId, URI organizationId,
                                                                        ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, ResourceUpdateEvent.Action.REMOVED, claim);
    }

    public static ResourceUpdateEvent<ChannelClaim> updatedChannelClaim(URI customerId, URI organizationId,
                                                                        ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, ResourceUpdateEvent.Action.UPDATED, claim);
    }

    private static ResourceUpdateEvent<ChannelClaim> channelClaimEvent(URI customerId,
                                                                       URI organizationId,
                                                                       ResourceUpdateEvent.Action action,
                                                                       ChannelClaimDto claim) {
        var constraint =
            new Constraints(EnumSet.copyOf(claim.constraint().scope()),
                            claim.constraint().publishingPolicy(),
                            claim.constraint().editingPolicy());
        var channelClaim = new ChannelClaim(ChannelClaimIdProducer.channelClaimId(claim),
                                            claim.channel(),
                                            customerId,
                                            organizationId,
                                            constraint);
        return new ResourceUpdateEvent<>(action, RESOURCE_TYPE_CHANNEL_CLAIM, channelClaim);
    }
}
