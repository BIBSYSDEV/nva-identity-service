package no.unit.nva.useraccessmanagement.dao;

import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.core.JsonSerializable;

public interface DynamoEntryWithRangeKey extends WithType, JsonSerializable {

    String FIELD_DELIMITER = "#";

    //    @DynamoDbPartitionKey
    //    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    String getPrimaryKeyHashKey();

    /**
     * Setter of the primary hash key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary hash key.
     */
    void setPrimaryKeyHashKey(String primaryRangeKey);

    //    @DynamoDbSortKey
    //    @DynamoDbAttribute(DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY)
    String getPrimaryKeyRangeKey();

    /**
     * Setter of the primary range key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary range key.
     */
    void setPrimaryKeyRangeKey(String primaryRangeKey);

}