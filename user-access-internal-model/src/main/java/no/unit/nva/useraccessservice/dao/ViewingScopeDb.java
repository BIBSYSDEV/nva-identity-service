package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.useraccessservice.dao.DynamoEntriesUtils.nonEmpty;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
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
    
    public static final String EXCLUDED_UNITS = "excludedUnits";
    public static final String INCLUDED_UNITS = "includedUnits";
    public static final String VIEWING_SCOPE_TYPE = "ViewingScope";
    
    private Set<URI> includedUnits;
    private Set<URI> excludedUnits;
    
    public ViewingScopeDb() {
    }
    
    public ViewingScopeDb(Set<URI> includedUnits, Set<URI> excludedUnits) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
        this.excludedUnits = nonEmptyOrDefault(excludedUnits);
    }
    
    public static ViewingScopeDb fromViewingScope(ViewingScope dto) {
        return Optional.ofNullable(dto).map(ViewingScopeDb::fromDto).orElse(null);
    }
    
    public ViewingScope toViewingScope() {
        return attempt(() -> ViewingScope.create(getIncludedUnits(), getExcludedUnits())).orElseThrow();
    }
    
    @DynamoDbAttribute(INCLUDED_UNITS)
    @DynamoDbIgnoreNulls
    public Set<URI> getIncludedUnits() {
        return nonEmptyOrDefault(includedUnits);
    }
    
    public void setIncludedUnits(Set<URI> includedUnits) {
        this.includedUnits = nonEmptyOrDefault(includedUnits);
    }
    
    @DynamoDbAttribute(EXCLUDED_UNITS)
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
    public void setType(String type) throws BadRequestException {
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
        if (!(o instanceof ViewingScopeDb that)) {
            return false;
        }
        return Objects.equals(getIncludedUnits(), that.getIncludedUnits())
               && Objects.equals(getExcludedUnits(), that.getExcludedUnits());
    }

    private static ViewingScopeDb fromDto(ViewingScope dto) {
        var dao = new ViewingScopeDb();
        dao.setExcludedUnits(dto.getExcludedUnits());
        dao.setIncludedUnits(dto.getIncludedUnits());
        return dao;
    }

    private Set<URI> nonEmptyOrDefault(Set<URI> units) {
        return nonEmpty(units) ? units : null;
    }
}
