package no.unit.nva.customer.model;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RetentionStrategy;
import nva.commons.core.JacocoGenerated;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RetentionStrategyDto implements RetentionStrategy, JsonSerializable {

    private final RetentionStrategyType retentionStrategy;
    private final URI id;

    @ConstructorProperties({"retentionStrategy", "id"})
    public RetentionStrategyDto(RetentionStrategyType retentionStrategy, URI id) {
        this.retentionStrategy = retentionStrategy;
        this.id = id;
    }

    public RetentionStrategyDto(RetentionStrategy retention) {
        this(retention.getRetentionStrategy(), retention.getId());
    }

    @Override
    public RetentionStrategyType getRetentionStrategy() {
        return retentionStrategy;
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
        RetentionStrategyDto that = (RetentionStrategyDto) o;
        return retentionStrategy == that.retentionStrategy && Objects.equals(id, that.id);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(retentionStrategy, id);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
