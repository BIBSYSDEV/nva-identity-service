package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.model.RoleDto.Builder;
import no.unit.nva.useraccessmanagement.model.interfaces.Validable;
import nva.commons.core.JacocoGenerated;

public class RoleDto implements WithCopy<Builder>, Validable, Typed {

    public static final String TYPE = "Role";
    public static final String MISSING_ROLE_NAME_ERROR = "Role should have a name";
    @JsonProperty("rolename")
    private String roleName;
    @JsonProperty("accessRights")
    private Set<String> accessRights;

    public RoleDto() {
        accessRights = Collections.emptySet();
    }

    public static Builder newBuilder() {
        return new RoleDto.Builder();
    }

    @Override
    public Builder copy() throws InvalidInputException {
        return new Builder()
            .withAccessRights(this.getAccessRights())
            .withRoleName(this.getRoleName());
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {

        this.roleName = roleName;
    }

    public List<String> getAccessRights() {
        return nonNull(accessRights) ? new ArrayList<>(accessRights) : Collections.emptyList();
    }

    public void setAccessRights(List<String> accessRights) {
        this.accessRights = nonNull(accessRights) ? new HashSet<>(accessRights) : Collections.emptySet();
    }

    @Override
    public boolean isValid() {
        return !(isNull(this.getRoleName()) || this.getRoleName().isBlank());
    }

    @Override
    public InvalidInputException exceptionWhenInvalid() {
        return new InvalidInputException(MISSING_ROLE_NAME_ERROR);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getRoleName(), getAccessRights());
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
        RoleDto roleDto = (RoleDto) o;
        return Objects.equals(getRoleName(), roleDto.getRoleName())
               && Objects.equals(accessRightsAsSet(), roleDto.accessRightsAsSet());
    }

    @Override
    public String toString() {
        return attempt(() -> JSON.std.asString(this)).orElseThrow();
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return RoleDto.TYPE;
    }

    @Override
    public void setType(String type) {
        Typed.super.setType(type);
    }

    // Access Rights are a Set but JacksonJr does not support sets.
    private Set<String> accessRightsAsSet() {
        return JacksonJrDoesNotSupportSets.toSet(accessRights);
    }

    public static final class Builder {

        private final RoleDto roleDto;

        private Builder() {
            roleDto = new RoleDto();
        }

        public Builder withRoleName(String roleName) {
            roleDto.setRoleName(roleName);
            if (roleDto.isInvalid()) {
                throw new InvalidEntryInternalException(MISSING_ROLE_NAME_ERROR);
            }
            return this;
        }

        public Builder withAccessRights(Collection<String> accessRights) {
            roleDto.setAccessRights(new ArrayList<>(accessRights));
            return this;
        }

        public RoleDto build() {
            return roleDto;
        }
    }
}
