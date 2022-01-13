package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
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
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@DynamoDbBean
public class RoleDb implements DynamoEntryWithRangeKey, WithCopy<Builder>, WithType, JsonSerializable {

    public static String TYPE = "ROLE";
    private Set<AccessRight> accessRights;
    private String name;
    public static StaticTableSchema<RoleDb> TABLE_SCHEMA = StaticTableSchema
        .builder(RoleDb.class)
        .newItemSupplier(RoleDb::new)
        .addAttribute(String.class,
                      at -> at.name("name")
                          .getter(RoleDb::getName)
                          .setter(RoleDb::setName))
        .addAttribute(EnhancedType.setOf(AccessRight.class),
                      at -> at.name("accessRights")
                          .getter(RoleDb::getAccessRights)
                          .setter(RoleDb::setAccessRights)
        )
        .addAttribute(String.class,
                      at -> at.name(PRIMARY_KEY_HASH_KEY)
                          .getter(RoleDb::getPrimaryKeyHashKey)
                          .setter(RoleDb::setPrimaryKeyHashKey)
                          .addTag(StaticAttributeTags.primaryPartitionKey())
        )
        .addAttribute(String.class,
                      at -> at.name(PRIMARY_KEY_RANGE_KEY)
                          .getter(RoleDb::getPrimaryKeyRangeKey)
                          .setter(RoleDb::setPrimaryKeyRangeKey)
                          .addTag(StaticAttributeTags.primarySortKey())
        )
        .addAttribute(String.class,
                      at -> at.name("type")
                          .getter(RoleDb::getType)
                          .setter(RoleDb::setType)
        )
        .build();

    public static final AttributeConverter<Set<RoleDb>> CONVERTER = new AttributeConverter<>() {
        @Override
        public AttributeValue transformFrom(Set<RoleDb> input) {
            var items = input.stream()
                .map(role -> RoleDb.TABLE_SCHEMA.itemToMap(role, true))
                .map(role -> AttributeValue.builder().m(role).build())
                .collect(Collectors.toList());
            return AttributeValue.builder().l(items).build();
        }

        @Override
        public Set<RoleDb> transformTo(AttributeValue input) {
            if (input.hasL()) {
                return input.l().stream()
                    .map(AttributeValue::m)
                    .map(map -> RoleDb.TABLE_SCHEMA.mapToItem(map))
                    .collect(Collectors.toSet());
            }
            return Collections.emptySet();
        }

        @JacocoGenerated
        @Override
        public EnhancedType<Set<RoleDb>> type() {
            return EnhancedType.setOf(RoleDb.class);
        }

        @JacocoGenerated
        @Override
        public AttributeValueType attributeValueType() {
            return AttributeValueType.BS;
        }
    };

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

    @DynamoDbAttribute("accessRights")
    @DynamoDbIgnoreNulls
    public Set<AccessRight> getAccessRights() {
        return nonNull(this.accessRights) && !this.accessRights.isEmpty()
                   ? accessRights
                   : null;
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
        RoleDb roleDbEntry = (RoleDb) o;
        return Objects.equals(getAccessRights(), roleDbEntry.getAccessRights())
               && Objects.equals(getName(), roleDbEntry.getName());
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }

    public RoleDto toRoleDto() {
        Set<String> accessRightsStrings =
            Optional.ofNullable(this.getAccessRights())
                .stream()
                .flatMap(Collection::stream)
                .map(AccessRight::toString)
                .collect(Collectors.toSet());
        return Try.attempt(() -> RoleDto.newBuilder()
                .withName(this.getName())
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
