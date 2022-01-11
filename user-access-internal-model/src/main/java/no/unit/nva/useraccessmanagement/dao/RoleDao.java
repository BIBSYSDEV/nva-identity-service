package no.unit.nva.useraccessmanagement.dao;

import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import java.util.Set;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class RoleDao extends RoleDb implements DynamoEntryWithRangeKey {

    public RoleDao() {

    }

    public RoleDao(RoleDb entry) {
        RoleDao roleDbEntry = new RoleDao();
        roleDbEntry.setName(entry.getName());
        roleDbEntry.setAccessRights(entry.getAccessRights());
    }

    public static RoleDao fromRoleDto(RoleDto dto){
        return RoleDb.fromRoleDto(dto).toRoleDao();
    }


    public static RoleDao.Builder newBuilder() {
        return new RoleDao.Builder();
    }

    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return this.getType() + FIELD_DELIMITER + getName();
    }

    @Override
    @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
    @DynamoDbSortKey
    public String getPrimaryKeyRangeKey() {
        return this.getPrimaryKeyHashKey();
    }

    @Override
    public RoleDao.Builder copy() {
        return new RoleDao.Builder()
            .withName(this.getName())
            .withAccessRights(this.getAccessRights());
    }

    public RoleDb toRoleDb() {
        return this;
    }

    public static class Builder extends RoleDb.Builder {

        private RoleDao roleDbEntry;

        protected Builder() {
            roleDbEntry = new RoleDao();
        }

        public Builder withName(String name) {
            this.roleDbEntry.setName(name);
            return this;
        }

        public Builder withAccessRights(Set<AccessRight> accessRights) {
            this.roleDbEntry.setAccessRights(accessRights);
            return this;
        }

        public RoleDao build() {
            return this.roleDbEntry;
        }
    }
}
