package no.unit.nva.useraccessservice.dao;

import no.unit.nva.useraccessservice.dao.RoleDb.Builder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.dao.DynamoEntriesUtils.nonEmpty;

@DynamoDbBean(converterProviders = {AccessRightSetConverterProvider.class, DefaultAttributeConverterProvider.class})
public class RoleDb implements DynamoEntryWithRangeKey, WithCopy<Builder>, Typed {

    public static final String NAME_FIELD = "name";
    public static final String ACCESS_RIGHTS_FIELDS = "accessRights";

    public static final String TYPE_VALUE = "ROLE";
    public static final TableSchema<RoleDb> TABLE_SCHEMA = TableSchema.fromBean(RoleDb.class);
    private Set<AccessRight> accessRights;
    private RoleName name;

    public RoleDb() {
        super();
        this.accessRights = Collections.emptySet();
    }

    @SuppressWarnings("PMD.NullAssignment") // FIXME: Do we really need to set accessRights to null?
    private RoleDb(Builder builder) {
        super();
        this.name = builder.name;
        this.accessRights = nonEmpty(builder.accessRights) ? builder.accessRights : null;
    }

    /**
     * Creates a DAO from a DTO.
     *
     * @param roleDto the dto.
     * @return the dao
     */
    public static RoleDb fromRoleDto(RoleDto roleDto) {

        return newBuilder()
            .withName(roleDto.getRoleName())
            .withAccessRights(roleDto.getAccessRights())
            .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Builder copy() {
        return new Builder()
            .withName(this.getName())
            .withAccessRights(this.getAccessRights());
    }    @JacocoGenerated
    @DynamoDbAttribute(NAME_FIELD)
    public RoleName getName() {
        return name;
    }

    @DynamoDbAttribute(ACCESS_RIGHTS_FIELDS)
    @DynamoDbIgnoreNulls
    public Set<AccessRight> getAccessRights() {
        return nonEmpty(accessRights) ? accessRights : Collections.emptySet();
    }    @JacocoGenerated
    public void setName(RoleName name) {
        this.name = name;
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void setAccessRights(Set<AccessRight> accessRights) {
        this.accessRights = nonEmpty(accessRights) ? accessRights : null;
    }    @JacocoGenerated
    @DynamoDbAttribute(TYPE_FIELD)
    @Override
    public String getType() {
        return TYPE_VALUE;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getAccessRights(), getName());
    }    @Override
    public void setType(String ignored) {
        //DO NOTHING
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
    }    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return this.getType() + FIELD_DELIMITER + getName().getValue();
    }

    public RoleDto toRoleDto() {
        return RoleDto.newBuilder()
            .withRoleName(this.getName())
            .withAccessRights(getAccessRights())
            .build();
    }    @Override
    public void setPrimaryKeyHashKey(String primaryRangeKey) {
        //DO NOTHING
    }

    public static class Builder {

        public static final String EMPTY_ROLE_NAME_ERROR = "Rolename cannot be null or blank";
        private RoleName name;
        private Set<AccessRight> accessRights;

        protected Builder() {
            accessRights = Collections.emptySet();
        }

        public Builder withName(RoleName name) {
            this.name = name;
            return this;
        }

        public Builder withAccessRights(Set<AccessRight> accessRights) {
            this.accessRights = nonNull(accessRights) ? accessRights : Collections.emptySet();
            return this;
        }

        public RoleDb build() {
            if (nonNull(name) && StringUtils.isNotBlank(name.getValue())) {
                return new RoleDb(this);
            }
            throw new InvalidEntryInternalException(EMPTY_ROLE_NAME_ERROR);
        }
    }    @Override
    @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
    @DynamoDbSortKey
    public String getPrimaryKeyRangeKey() {
        return this.getPrimaryKeyHashKey();
    }

    @Override
    public void setPrimaryKeyRangeKey(String primaryRangeKey) {
        //DO NOTHING
    }














}
