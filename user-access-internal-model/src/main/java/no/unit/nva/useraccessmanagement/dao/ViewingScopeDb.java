package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.isNull;
import static no.unit.nva.useraccessmanagement.dao.DynamoEntriesUtils.nonEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.useraccessmanagement.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;

/**
 * This is a Curator's default viewing scope expressed as a set of included and excluded Cristin Units. The Curator's
 * viewing scope has meaning only if a user is a Curator or has seme specific access rights. Otherwise, it has no effect
 * on what the user can do. The Curator's view does not give the Curator some extra rights.It is only a default view
 * that the Curator can change at will as they are using the service. Note: The Curator can change their view when they
 * are using the service (i.e., per query) but not their default ViewingScope as this has been set by some
 * Administrator.
 */

@DynamoDbBean
public class ViewingScopeDb implements Typed {

    public static final String EXCLUDED_UNIS = "excludedUnis";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String VIEWING_SCOPE_TYPE = "ViewingScope";

    @JsonProperty(INCLUDED_UNITS)
    private Set<URI> includedUnits;
    @JsonProperty(EXCLUDED_UNIS)
    private Set<URI> excludedUnits;

    public ViewingScopeDb() {
    }

    public ViewingScopeDb(Set<URI> includedUnits, Set<URI> excludedUnits) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
        validate(includedUnits);
    }

    public static ViewingScopeDb fromViewingScope(ViewingScope dto) {
        return Optional.ofNullable(dto).map(ViewingScopeDb::fromDto).orElse(null);
    }

    public ViewingScope toViewingScope() {
        return attempt(() -> new ViewingScope(getIncludedUnits(), getExcludedUnits())).orElseThrow();
    }

    @DynamoDbAttribute(INCLUDED_UNITS)
    @DynamoDbIgnoreNulls
    public Set<URI> getIncludedUnits() {
        return nonEmptyOrDefault(includedUnits);
    }

    public void setIncludedUnits(Set<URI> includedUnits) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
    }

    @DynamoDbAttribute(EXCLUDED_UNIS)
    public Set<URI> getExcludedUnits() {
        return nonEmptyOrDefault(excludedUnits);
    }

    public void setExcludedUnits(Set<URI> excludedUnits) {
        this.excludedUnits = excludedUnits;
    }

    @Override
    public String getType() {
        return VIEWING_SCOPE_TYPE;
    }

    @Override
    @JacocoGenerated
    public void setType(String type) {
        Typed.super.setType(type);
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
        if (!(o instanceof ViewingScopeDb)) {
            return false;
        }
        ViewingScopeDb that = (ViewingScopeDb) o;
        return Objects.equals(getIncludedUnits(), that.getIncludedUnits())
               && Objects.equals(getExcludedUnits(), that.getExcludedUnits());
    }

    private static ViewingScopeDb fromDto(ViewingScope dto) {
        var dao = new ViewingScopeDb();
        dao.setExcludedUnits(JacksonJrDoesNotSupportSets.toSet(dto.getExcludedUnits()));
        dao.setIncludedUnits(JacksonJrDoesNotSupportSets.toSet(dto.getIncludedUnits()));
        attempt(() -> validate(dao.getIncludedUnits())).orElseThrow();
        return dao;
    }

    private static Void validate(Set<URI> includedUnits)  {
        if (isNull(includedUnits) || includedUnits.isEmpty()) {
            throw new BadRequestException("Invalid Viewing Scope: \"includedUnits\" cannot be empty");
        }
        return null;
    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonEmpty(units) ? units : null;
    }
}
