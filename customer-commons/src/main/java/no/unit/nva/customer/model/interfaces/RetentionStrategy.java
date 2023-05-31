package no.unit.nva.customer.model.interfaces;

import java.net.URI;
import no.unit.nva.customer.model.RetentionStrategyType;

public interface RetentionStrategy {
    RetentionStrategyType getRetentionStrategy();

    URI getId();

}
