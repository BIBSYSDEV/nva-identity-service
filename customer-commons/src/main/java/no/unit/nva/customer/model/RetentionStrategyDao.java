package no.unit.nva.customer.model;

import java.net.URI;
import java.util.Objects;
import javax.management.ConstructorParameters;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RetentionStrategy;
import nva.commons.core.JacocoGenerated;

public class RetentionStrategyDao implements RetentionStrategy, JsonSerializable {
    private final RetentionStrategyType retentionStrategyType;
    private final URI id;

    @ConstructorParameters({"retentionStrategy","id"})
    public RetentionStrategyDao(RetentionStrategyType retentionStrategyType, URI id) {
        this.retentionStrategyType = retentionStrategyType;
        this.id = id;
    }

    public RetentionStrategyDao(RetentionStrategy retention) {
        this(retention.getRetentionStrategy(), retention.getId());
    }

    public RetentionStrategyType getRetentionStrategy() {
        return retentionStrategyType;
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
        return retentionStrategyType == that.retentionStrategyType && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(retentionStrategyType, id);
    }

    @Override
    @JacocoGenerated
    public String toString(){
        return toJsonString();
    }

}
