package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.dao.RoleDb.Builder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class RoleDb implements WithCopy<Builder>, WithType, JsonSerializable {

    public static String TYPE = "ROLE";
    public static final String INVALID_PRIMARY_HASH_KEY = "PrimaryHashKey should start with \"" + TYPE + "\"";
    public static final String INVALID_PRIMARY_RANGE_KEY = "PrimaryHashKey should start with \"" + TYPE + "\"";

    private Set<AccessRight> accessRights;
    private String name;

    public RoleDb() {
        super();
        this.accessRights = Collections.emptySet();
    }

    private RoleDb(Builder builder) {
        super();
        setName(builder.name);
        setAccessRights(builder.accessRights);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a DAO from a DTO.
     *
     * @param roleDto the dto.
     * @return the dao
     */
    public static RoleDb fromRoleDto(RoleDto roleDto) {
        Set<AccessRight> accessRights = roleDto.getAccessRights()
            .stream()
            .map(AccessRight::fromString)
            .collect(Collectors.toSet());

        return RoleDb.newBuilder()
            .withName(roleDto.getRoleName())
            .withAccessRights(accessRights)
            .build();
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    @JacocoGenerated
    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    @JsonProperty("type")
    @Override
    public String getType() {
        return TYPE;
    }

    public Set<AccessRight> getAccessRights() {
        return Objects.nonNull(this.accessRights) ? accessRights : Collections.emptySet();
    }

    public void setAccessRights(Set<AccessRight> accessRights) {
        this.accessRights = accessRights;
    }

    @Override
    public Builder copy() {
        return new Builder()
            .withName(this.getName())
            .withAccessRights(this.getAccessRights());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getAccessRights(), getName());
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
        RoleDao roleDbEntry = (RoleDao) o;
        return Objects.equals(getAccessRights(), roleDbEntry.getAccessRights())
               && Objects.equals(getName(), roleDbEntry.getName());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }

    public RoleDto toRoleDto() {
        Set<String> accessRightsStrings = this.getAccessRights().stream()
            .map(AccessRight::toString)
            .collect(Collectors.toSet());
        return Try.attempt(() -> RoleDto.newBuilder()
                .withName(this.getName())
                .withAccessRights(accessRightsStrings)
                .build())
            .orElseThrow(fail -> new InvalidEntryInternalException(fail.getException()));
    }

    public RoleDao toRoleDao() {
        return new RoleDao(this);
    }

    public static class Builder {

        public static final String EMPTY_ROLE_NAME_ERROR = "Rolename cannot be null or blank";
        private String name;
        private Set<AccessRight> accessRights;

        protected Builder() {
            accessRights = Collections.emptySet();
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withAccessRights(Set<AccessRight> accessRights) {
            this.accessRights = nonNull(accessRights) ? accessRights : Collections.emptySet();
            return this;
        }

        public RoleDb build() {
            return new RoleDb(this);
        }
    }
}
