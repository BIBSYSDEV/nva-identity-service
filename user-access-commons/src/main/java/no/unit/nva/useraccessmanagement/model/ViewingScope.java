package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

/**
 * This is a Curator's default viewing scope expressed as a set of included and excluded Cristin Units. The Curator's
 * viewing scope has meaning only if a user is a Curator or has seme specific access rights. Otherwise, it has no effect
 * on what the user can do. The Curator's view does not give the Curator some extra rights.It is only a default view
 * that the Curator can change at will as they are using the service. Note: The Curator can change their view when they
 * are using the service (i.e., per query) but not their default ViewingScope as this has been set by some
 * Administrator.
 */
public class ViewingScope implements WithType {

    public static final String EXCLUDED_UNIS = "excludedUnis";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String VIEWING_SCOPE_TYPE = "ViewingScope";
    @JsonProperty(INCLUDED_UNITS)
    private final Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private final Set<URI> excludedUnits;

    @JsonCreator
    public ViewingScope(@JsonProperty(INCLUDED_UNITS) Set<URI> includedUnits,
                        @JsonProperty(EXCLUDED_UNIS) Set<URI> excludedUnits) throws BadRequestException {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
        validate(includedUnits);
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
        return VIEWING_SCOPE_TYPE;
    }

    private void validate(Set<URI> includedUnits) throws BadRequestException {
        if (includedUnits.isEmpty()) {
            throw new BadRequestException("Invalid Viewing Scope: \"includedUnits\" cannot be empty");
        }
    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonNull(units) ? units : Collections.emptySet();
    }
}
