package no.unit.nva.useraccessservice.dao;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.dao.DynamoEntriesUtils.nonEmpty;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.dao.RoleDb.Builder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class RoleDb implements DynamoEntryWithRangeKey, WithCopy<Builder>, Typed {

    public static final String NAME_FIELD = "name";
    public static final String ACCESS_RIGHTS_FIELDS = "accessRights";

    public static final String TYPE_VALUE = "ROLE";
    public static final TableSchema<RoleDb> TABLE_SCHEMA = TableSchema.fromBean(RoleDb.class);
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
    @DynamoDbAttribute(NAME_FIELD)
    public String getName() {
        return name;
    }

    @JacocoGenerated
    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    @DynamoDbAttribute(TYPE_FIELD)
    @Override
    public String getType() {
        return TYPE_VALUE;
    }

    @Override
    public void setType(String ignored){
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return this.getType() + FIELD_DELIMITER + getName();
    }

    @Override
    public void setPrimaryKeyHashKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @Override
    @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
    @DynamoDbSortKey
    public String getPrimaryKeyRangeKey() {
        return this.getPrimaryKeyHashKey();
    }

    @Override
    public void setPrimaryKeyRangeKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @DynamoDbAttribute(ACCESS_RIGHTS_FIELDS)
    @DynamoDbIgnoreNulls
    public Set<AccessRight> getAccessRights() {
        return nonEmpty(accessRights) ? accessRights : null;
    }

    @DynamoDbIgnore
    @JacocoGenerated
    public Set<AccessRight> getAccessRightsNonNull() {
        return nonNull(accessRights) ? accessRights : Collections.emptySet();
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void setAccessRights(Set<AccessRight> accessRights) {
        this.accessRights = nonEmpty(accessRights) ? accessRights : null;
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
        RoleDb roleDbEntry = (RoleDb) o;
        return Objects.equals(getAccessRights(), roleDbEntry.getAccessRights())
               && Objects.equals(getName(), roleDbEntry.getName());
    }

    public RoleDto toRoleDto() {
        Set<String> accessRightsStrings =
            Optional.ofNullable(this.getAccessRights())
                .stream()
                .flatMap(Collection::stream)
                .map(AccessRight::toString)
                .collect(Collectors.toSet());
        return Try.attempt(() -> RoleDto.newBuilder()
                .withRoleName(this.getName())
                .withAccessRights(accessRightsStrings)
                .build())
            .orElseThrow(fail -> new InvalidEntryInternalException(fail.getException()));
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
            if (StringUtils.isNotBlank(name)) {
                return new RoleDb(this);
            }
            throw new InvalidEntryInternalException(EMPTY_ROLE_NAME_ERROR);
        }
    }
}