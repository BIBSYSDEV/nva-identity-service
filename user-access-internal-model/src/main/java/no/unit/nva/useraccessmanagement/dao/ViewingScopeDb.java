package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * This is a Curator's default viewing scope expressed as a set of included and excluded Cristin Units. The Curator's
 * viewing scope has meaning only if a user is a Curator or has seme specific access rights. Otherwise, it has no effect
 * on what the user can do. The Curator's view does not give the Curator some extra rights.It is only a default view
 * that the Curator can change at will as they are using the service. Note: The Curator can change their view when they
 * are using the service (i.e., per query) but not their default ViewingScope as this has been set by some
 * Administrator.
 */

@DynamoDbBean
public class ViewingScopeDb implements WithType {

    public static final String EXCLUDED_UNIS = "excludedUnis";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String VIEWING_SCOPE_TYPE = "ViewingScope";
    public static final boolean INCLUDE_NESTED_UNITS = true;
    public static final boolean DO_NOT_INCLUDE_NESTED_UNITS = !INCLUDE_NESTED_UNITS;
    private static final String NESTED_UNITS = "recursive";
    @JsonProperty(NESTED_UNITS)
    private boolean recursive;
    @JsonProperty(INCLUDED_UNITS)
    private Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private Set<URI> excludedUnits;

    public ViewingScopeDb() {
    }

    public ViewingScopeDb(Set<URI> includedUnits,
                          Set<URI> excludedUnits,
                          Boolean recursive)

        throws BadRequestException {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
        this.recursive = Optional.ofNullable(recursive).orElse(false);
        validate(includedUnits);
    }

    public static ViewingScopeDb fromViewingScope(ViewingScope dto) {
        return Optional.ofNullable(dto).map(ViewingScopeDb::fromDto).orElse(null);
    }

    public ViewingScope toViewingScope() {
        return attempt(() -> new ViewingScope(getIncludedUnits(), getExcludedUnits(), isRecursive())).orElseThrow();
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public Set<URI> getIncludedUnits() {
        return includedUnits;
    }

    public void setIncludedUnits(Set<URI> includedUnits) {
        this.includedUnits = includedUnits;
    }

    public Set<URI> getExcludedUnits() {
        return excludedUnits;
    }

    public void setExcludedUnits(Set<URI> excludedUnits) {
        this.excludedUnits = excludedUnits;
    }

    @Override
    public String getType() {
        return VIEWING_SCOPE_TYPE;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViewingScopeDb)) {
            return false;
        }
        ViewingScopeDb that = (ViewingScopeDb) o;
        return isRecursive() == that.isRecursive()
               && Objects.equals(getIncludedUnits(), that.getIncludedUnits())
               && Objects.equals(getExcludedUnits(), that.getExcludedUnits());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(isRecursive(), getIncludedUnits(), getExcludedUnits());
    }

    private static ViewingScopeDb fromDto(ViewingScope dto) {
        var dao = new ViewingScopeDb();
        dao.setExcludedUnits(dto.getExcludedUnits());
        dao.setIncludedUnits(dto.getIncludedUnits());
        dao.setRecursive(dto.isRecursive());
        attempt(() -> validate(dao.getIncludedUnits())).orElseThrow();
        return dao;
    }

    private static Void validate(Set<URI> includedUnits) throws BadRequestException {
        if (includedUnits.isEmpty()) {
            throw new BadRequestException("Invalid Viewing Scope: \"includedUnits\" cannot be empty");
        }
        return null;
    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonNull(units) ? units : Collections.emptySet();
    }
}
