package no.unit.nva.customer.model;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import nva.commons.core.JacocoGenerated;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.util.Objects;

public class RightsRetentionStrategyDto implements RightsRetentionStrategy, JsonSerializable {

    private final RightsRetentionStrategyType type;
    private final URI id;

    @ConstructorProperties({"type", "id"})
    public RightsRetentionStrategyDto(RightsRetentionStrategyType type, URI id) {
        this.type = type;
        this.id = id;
    }

    public RightsRetentionStrategyDto(RightsRetentionStrategy retention) {
        this(retention.getType(), retention.getId());
    }

    @Override
    public RightsRetentionStrategyType getType() {
        return type;
    }

    @Override
    public URI getId() {
        return id;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RightsRetentionStrategyDto that = (RightsRetentionStrategyDto) o;
        return type == that.type && Objects.equals(id, that.id);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
