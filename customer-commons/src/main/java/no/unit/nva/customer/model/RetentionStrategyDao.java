package no.unit.nva.customer.model;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RetentionStrategy;
import nva.commons.core.JacocoGenerated;

public class RetentionStrategyDao implements RetentionStrategy, JsonSerializable {

    private final RetentionStrategyType retentionStrategy;
    private final URI id;

    @ConstructorProperties({"retentionStrategy", "id"})
    public RetentionStrategyDao(RetentionStrategyType retentionStrategy, URI id) {
        this.retentionStrategy = retentionStrategy;
        this.id = id;
    }

    public RetentionStrategyDao(RetentionStrategy retention) {
        this(retention.getRetentionStrategy(), retention.getId());
    }

    public RetentionStrategyType getRetentionStrategy() {
        return retentionStrategy;
    }

    public URI getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RetentionStrategyDao that = (RetentionStrategyDao) o;
        return retentionStrategy == that.retentionStrategy && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(retentionStrategy, id);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
