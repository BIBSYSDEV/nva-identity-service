package no.unit.nva.customer.events.model;

import static no.unit.nva.customer.events.model.ResourceUpdateEvent.Action.ADDED;
import static no.unit.nva.customer.events.model.ResourceUpdateEvent.Action.REMOVED;
import static no.unit.nva.customer.events.model.ResourceUpdateEvent.Action.UPDATED;
import java.net.URI;
import java.util.EnumSet;
import no.unit.nva.customer.ChannelClaimIdProducer;
import no.unit.nva.customer.events.model.ChannelClaim.Constraints;
import no.unit.nva.customer.events.model.ResourceUpdateEvent.Action;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;

public final class ChannelClaimUpdateEvents {

    private static final String RESOURCE_TYPE_CHANNEL_CLAIM = "ChannelClaim";

    private ChannelClaimUpdateEvents() {
    }

    public static ResourceUpdateEvent<ChannelClaim> addedChannelClaim(URI customerId, URI organizationId,
                                                                      ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, ADDED, claim);
    }

    public static ResourceUpdateEvent<ChannelClaim> removedChannelClaim(URI customerId, URI organizationId,
                                                                        ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, REMOVED, claim);
    }

    public static ResourceUpdateEvent<ChannelClaim> updatedChannelClaim(URI customerId, URI organizationId,
                                                                        ChannelClaimDto claim) {
        return channelClaimEvent(customerId, organizationId, UPDATED, claim);
    }

    private static ResourceUpdateEvent<ChannelClaim> channelClaimEvent(URI customerId,
                                                                       URI organizationId,
                                                                       Action action,
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
