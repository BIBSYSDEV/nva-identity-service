package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

/**
 * This is a Curator's default viewing scope expressed as a set of included and excluded Cristin Units. The Curator's
 * viewing scope has meaning only if a user is a Curator or has seme specific access rights. Otherwise, it has no effect
 * on what the user can do. The Curator's view does not give the Curator some extra rights.It is only a default view
 * that the Curator can change at will as they are using the service. Note: The Curator can change their view when they
 * are using the service (i.e., per query) but not their default ViewingScope as this has been set by some
 * Administrator.
 */

@JsonTypeName(ViewingScope.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public class ViewingScope {

    public static final String EXCLUDED_UNITS = "excludedUnits";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String TYPE = "ViewingScope";

    @JsonProperty(INCLUDED_UNITS)
    private final Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNITS)
    private final Set<URI> excludedUnits;

    @JsonCreator
    public ViewingScope(@JsonProperty(INCLUDED_UNITS) Set<URI> includedUnits,
                        @JsonProperty(EXCLUDED_UNITS) Set<URI> excludedUnits) throws BadRequestException {

        this.includedUnits = toSet(includedUnits);
        this.excludedUnits = toSet(excludedUnits);
        validate(this.includedUnits);
        validate(this.excludedUnits);
    }

    private ViewingScope() {

        this.includedUnits = Set.of();
        this.excludedUnits = Set.of();
    }

    public static ViewingScope defaultViewingScope(URI organizationId) {
        attempt(() -> validate(organizationId)).orElseThrow();
        return attempt(() -> ViewingScope.create(Set.of(organizationId), Collections.emptySet())).orElseThrow();
    }

    public static ViewingScope emptyViewingScope() {
        return new ViewingScope();
    }

    public static ViewingScope create(Collection<URI> includedUnits, Collection<URI> excludedUnits)
            throws BadRequestException {
        return new ViewingScope(toSet(includedUnits), toSet(excludedUnits));
    }

    public static ViewingScope fromJson(String input) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(input, ViewingScope.class)).orElseThrow(
                fail -> new BadRequestException("Could not read viewing scope:" + input));
    }

    public static Void validate(URI uri) throws BadRequestException {
        if (pathIsNotExpectedPath(uri)) {
            throw new BadRequestException("Unexpected path in Viewing Scope URI:" + uri);
        }
        if (hostIsNotExpectedHost(uri)) {
            throw new BadRequestException("Unexpected host in Viewing Scope URI:" + uri);
        }
        return null;
    }

    @JacocoGenerated
    public Set<URI> getIncludedUnits() {
        return includedUnits;
    }

    @JacocoGenerated
    public Set<URI> getExcludedUnits() {
        return excludedUnits;
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
        if (!(o instanceof ViewingScope that)) {
            return false;
        }
        return Objects.equals(getIncludedUnits(), that.getIncludedUnits()) && Objects.equals(getExcludedUnits(),
                that.getExcludedUnits());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    private static Set<URI> toSet(Collection<URI> collection) {
        return nonNull(collection) ? new HashSet<>(collection) : Collections.emptySet();
    }

    private static boolean pathIsNotExpectedPath(URI uri) {
        return !uri.getPath().startsWith(ServiceConstants.CRISTIN_PATH);
    }

    private static boolean hostIsNotExpectedHost(URI uri) {
        return !ServiceConstants.API_DOMAIN.equals(uri.getHost());
    }

    @JacocoGenerated
    private void validate(Collection<URI> uris) throws BadRequestException {
        for (URI uri : uris) {
            validate(uri);
        }
    }
}
