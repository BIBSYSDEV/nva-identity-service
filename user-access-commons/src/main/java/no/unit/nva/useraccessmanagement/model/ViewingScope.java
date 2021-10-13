package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.core.JacocoGenerated;

public class ViewingScope implements WithType {

    public static final String EXCLUDED_UNIS = "excludedUnis";
    public static final String INCLUDED_UNITS = "includedUnits";
    @JsonProperty(INCLUDED_UNITS)
    private final Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private final Set<URI> excludedUnits;

    @JsonCreator
    public ViewingScope(@JsonProperty(INCLUDED_UNITS) Set<URI> includedUnits,
                        @JsonProperty(EXCLUDED_UNIS) Set<URI> excludedUnits) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIncludedUnits(), getExcludedUnits());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViewingScope)) {
            return false;
        }
        ViewingScope that = (ViewingScope) o;
        return Objects.equals(getIncludedUnits(), that.getIncludedUnits())
               && Objects.equals(getExcludedUnits(), that.getExcludedUnits());
    }

    public Set<URI> getIncludedUnits() {
        return includedUnits;
    }

    public Set<URI> getExcludedUnits() {
        return excludedUnits;
    }

    @Override
    public String getType() {
        return "ViewingScope";
    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonNull(units) ? units : Collections.emptySet();
    }
}
