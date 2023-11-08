package no.unit.nva.customer.model.interfaces;

import java.net.URI;
import no.unit.nva.customer.model.RightsRetentionStrategyType;

public interface RightsRetentionStrategy {
    RightsRetentionStrategyType getRetentionStrategy();

    URI getId();

}
