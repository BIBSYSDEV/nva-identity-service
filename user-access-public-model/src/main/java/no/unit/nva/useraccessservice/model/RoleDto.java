package no.unit.nva.useraccessservice.model;

import static java.util.Objects.nonNull;
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
import java.util.Optional;
import java.util.Set;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.RoleDto.Builder;
import no.unit.nva.useraccessservice.model.interfaces.Validable;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class RoleDto implements WithCopy<Builder>, Validable, Typed {

    public static final String TYPE = "Role";
    public static final String MISSING_ROLE_NAME_ERROR = "Role should have a name";

    @JsonAlias("name")
    @JsonProperty("rolename")
    private RoleName roleName;
    @JsonProperty("accessRights")
    private Set<AccessRight> accessRights;

    public RoleDto() {
        accessRights = Collections.emptySet();
    }

    public static RoleDto fromJson(String json) throws BadRequestException, InvalidInputException {
        RoleDto roleDto = attempt(() -> JsonConfig.readValue(json, RoleDto.class))
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
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    @Override
    public Builder copy() {
        return new Builder()
            .withAccessRights(this.getAccessRights())
            .withRoleName(this.getRoleName());
    }

    public RoleName getRoleName() {
        return roleName;
    }

    public void setRoleName(RoleName roleName) {
        this.roleName = roleName;
    }

    public Set<AccessRight> getAccessRights() {
        return nonNull(accessRights) ? accessRights : Collections.emptySet();
    }

    public void setAccessRights(List<AccessRight> accessRights) {
        this.accessRights = nonNull(accessRights) ? new HashSet<>(accessRights) : Collections.emptySet();
    }

    @Override
    @JsonIgnore
    public boolean isValid() {
        return Optional.ofNullable(getRoleName()).map(RoleName::getValue).filter(value -> !value.isBlank()).isPresent();
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
               && Objects.equals(getAccessRights(), roleDto.getAccessRights());
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return RoleDto.TYPE;
    }

    @Override
    public void setType(String type) throws BadRequestException {
        Typed.super.setType(type);
    }


    public static final class Builder {

        private final RoleDto roleDto;

        private Builder() {
            roleDto = new RoleDto();
        }

        public Builder withRoleName(RoleName roleName) {
            roleDto.setRoleName(roleName);
            if (roleDto.isInvalid()) {
                throw new InvalidEntryInternalException(MISSING_ROLE_NAME_ERROR);
            }
            return this;
        }

        public Builder withAccessRights(Collection<AccessRight> accessRights) {
            roleDto.setAccessRights(new ArrayList<>(accessRights));
            return this;
        }

        public RoleDto build() {
            return roleDto;
        }
    }

    @JacocoGenerated
    @JsonIgnore
    public boolean isNotDeprecated() {
        return !getRoleName().isDeprecated();
    }
}
