package no.unit.nva.customer.model.interfaces;

import no.unit.nva.customer.model.RightsRetentionStrategyType;

import java.net.URI;

public interface RightsRetentionStrategy {
    RightsRetentionStrategyType getType();

    URI getId();

}
