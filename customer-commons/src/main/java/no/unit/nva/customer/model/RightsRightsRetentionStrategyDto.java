package no.unit.nva.customer.model;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.util.Objects;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import nva.commons.core.JacocoGenerated;

public class RightsRightsRetentionStrategyDto implements RightsRetentionStrategy, JsonSerializable {

    private final RightsRetentionStrategyType type;
    private final URI id;

    @ConstructorProperties({"type", "id"})
    public RightsRightsRetentionStrategyDto(RightsRetentionStrategyType type, URI id) {
        this.type = type;
        this.id = id;
    }

    public RightsRightsRetentionStrategyDto(RightsRetentionStrategy retention) {
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
        RightsRightsRetentionStrategyDto that = (RightsRightsRetentionStrategyDto) o;
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
