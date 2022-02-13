package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessmanagement.constants.ServiceConstants;
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
    public static final String INVALID_VIEWING_SCOPE_URI_ERROR = "Invalid Viewing Scope URI:";
    @JsonProperty(INCLUDED_UNITS)
    private final Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private final Set<URI> excludedUnits;

    public ViewingScope(@JsonProperty(INCLUDED_UNITS) Set<URI> includedUnits,
                        @JsonProperty(EXCLUDED_UNIS) Set<URI> excludedUnits) throws BadRequestException {

        this(includedUnits, excludedUnits, VIEWING_SCOPE_TYPE);
    }

    @JacocoGenerated
    @JsonCreator
    public ViewingScope(@JsonProperty(INCLUDED_UNITS) Set<URI> includedUnits,
                        @JsonProperty(EXCLUDED_UNIS) Set<URI> excludedUnits,
                        @JsonProperty(TYPE_FIELD) String type)

        throws BadRequestException {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
        if (!VIEWING_SCOPE_TYPE.equals(type)) {
            throw new BadRequestException("Expected type is " + VIEWING_SCOPE_TYPE);
        }
        //TODO: re-enable after migration
        //validate();
    }

    public static ViewingScope defaultViewingScope(URI organizationId) {
        attempt(() -> validate(organizationId)).orElseThrow();
        return attempt(() -> new ViewingScope(Set.of(organizationId), Collections.emptySet())).orElseThrow();
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

    @JacocoGenerated
    @Override
    public void setType(String type) {
        //DO NOTHING
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
    //TODO: re-enable after migration
    //    @JacocoGenerated
    //    private void validate() throws BadRequestException {
    //        if (includedUnits.isEmpty()) {
    //            throw new BadRequestException("Invalid Viewing Scope: \"includedUnits\" cannot be empty");
    //        }
    //        validate(includedUnits);
    //        validate(excludedUnits);
    //    }
    //
    //    //TODO: re-enable after migration
    //    @JacocoGenerated
    //    private void validate(Set<URI> uris) throws BadRequestException {
    //        for (URI uri : uris) {
    //            validate(uri);
    //        }
    //    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonNull(units) ? units : Collections.emptySet();
    }

    private static Void validate(URI uri) throws BadRequestException {
        if (isNotValidOrganizationId(uri)) {
            throw new BadRequestException(INVALID_VIEWING_SCOPE_URI_ERROR + uri);
        }
        return null;
    }

    public static boolean isNotValidOrganizationId(URI uri) {
        return pathIsNotExpectedPath(uri) || hostIsNotExpectedHost(uri);
    }

    private static boolean pathIsNotExpectedPath(URI uri) {
        return !uri.getPath().startsWith(ServiceConstants.CRISTIN_PATH);
    }

    private static boolean hostIsNotExpectedHost(URI uri) {
        return !ServiceConstants.API_HOST.equals(uri.getHost());
    }
}
