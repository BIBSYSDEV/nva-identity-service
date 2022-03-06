package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessmanagement.constants.ServiceConstants;
import no.unit.nva.useraccessmanagement.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

/**
 * This is a Curator's default viewing scope expressed as a set of included and excluded Cristin Units. The Curator's
 * viewing scope has meaning only if a user is a Curator or has seme specific access rights. Otherwise, it has no effect
 * on what the user can do. The Curator's view does not give the Curator some extra rights.It is only a default view
 * that the Curator can change at will as they are using the service. Note: The Curator can change their view when they
 * are using the service (i.e., per query) but not their default ViewingScope as this has been set by some
 * Administrator.
 */

public class ViewingScope implements Typed {

    public static final String EXCLUDED_UNIS = "excludedUnis";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String VIEWING_SCOPE_TYPE = "ViewingScope";
    public static final String INVALID_VIEWING_SCOPE_URI_ERROR = "Invalid Viewing Scope URI:";

    @JsonProperty(INCLUDED_UNITS)
    private List<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private List<URI> excludedUnits;

    @JacocoGenerated
    public ViewingScope() {

    }

    public ViewingScope(Set<URI> includedUnits, Set<URI> excludedUnits) {

        this(includedUnits, excludedUnits, VIEWING_SCOPE_TYPE);
    }

    private ViewingScope(Set<URI> includedUnits, Set<URI> excludedUnits, String type) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
        if (!VIEWING_SCOPE_TYPE.equals(type)) {
            throw new BadRequestException("Expected type is " + VIEWING_SCOPE_TYPE);
        }
        validate();
    }

    public static ViewingScope defaultViewingScope(URI organizationId) {
        attempt(() -> validate(organizationId)).orElseThrow();
        return attempt(() -> new ViewingScope(Set.of(organizationId), Collections.emptySet())).orElseThrow();
    }

    public static boolean isNotValidOrganizationId(URI uri) {
        return pathIsNotExpectedPath(uri) || hostIsNotExpectedHost(uri);
    }

    public static ViewingScope fromJson(String input) {
        return attempt(() -> objectMapper.beanFrom(ViewingScope.class, input))
            .orElseThrow(fail -> new BadRequestException("Could not read viewing scope:" + input));
    }

    @JacocoGenerated
    public List<URI> getIncludedUnits() {
        return JacksonJrDoesNotSupportSets.toList(includedUnits);
    }

    @JacocoGenerated
    public void setIncludedUnits(List<URI> includedUnits) {
        this.includedUnits = includedUnits;
    }

    @JacocoGenerated
    public List<URI> getExcludedUnits() {
        return excludedUnits;
    }

    @JacocoGenerated
    public void setExcludedUnits(List<URI> excludedUnits) {
        this.excludedUnits = excludedUnits;
    }

    @Override
    public String getType() {
        return VIEWING_SCOPE_TYPE;
    }

    @JacocoGenerated
    @Override
    public void setType(String type) {
        if (!VIEWING_SCOPE_TYPE.equals(type)) {
            throw new IllegalArgumentException("ViewingScope type is not " + VIEWING_SCOPE_TYPE);
        }
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

    @Override
    public String toString() {
        return attempt(() -> objectMapper.asString(this)).orElseThrow();
    }

    private static boolean pathIsNotExpectedPath(URI uri) {
        return !uri.getPath().startsWith(ServiceConstants.CRISTIN_PATH);
    }

    private static boolean hostIsNotExpectedHost(URI uri) {
        return !ServiceConstants.API_HOST.equals(uri.getHost());
    }

    private static Void validate(URI uri) {
        if (isNotValidOrganizationId(uri)) {
            throw new BadRequestException(INVALID_VIEWING_SCOPE_URI_ERROR + uri);
        }
        return null;
    }

    @JacocoGenerated
    private void validate() {
        if (includedUnits.isEmpty()) {
            throw new BadRequestException("Invalid Viewing Scope: \"includedUnits\" cannot be empty");
        }
        validate(includedUnits);
        validate(excludedUnits);
    }

    @JacocoGenerated
    private void validate(Collection<URI> uris) {
        for (URI uri : uris) {
            validate(uri);
        }
    }

    private List<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonNull(units) ? new ArrayList<>(units) : Collections.emptyList();
    }
}
