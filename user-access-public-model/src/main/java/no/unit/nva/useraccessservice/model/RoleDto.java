package no.unit.nva.useraccessservice.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.RoleDto.Builder;
import no.unit.nva.useraccessservice.model.interfaces.Validable;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class RoleDto implements WithCopy<Builder>, Validable, Typed {

    public static final String TYPE = "Role";
    public static final String MISSING_ROLE_NAME_ERROR = "Role should have a name";

    @JsonAlias("name")
    @JsonProperty("rolename")
    private String roleName;
    @JsonProperty("accessRights")
    private Set<String> accessRights;

    public RoleDto() {
        accessRights = Collections.emptySet();
    }

    public static RoleDto fromJson(String json) {
        RoleDto roleDto = attempt(() -> objectMapper.beanFrom(RoleDto.class, json))
            .orElseThrow(fail -> new BadRequestException("Could not parse role: " + json));
        if (roleDto.isInvalid()) {
            throw roleDto.exceptionWhenInvalid();
        }
        return roleDto;
    }

    public static Builder newBuilder() {
        return new RoleDto.Builder();
    }

    @Override
    public String toString() {
        return attempt(() -> objectMapper.asString(this)).orElseThrow();
    }

    @Override
    public Builder copy() {
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
    @JsonIgnore
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
