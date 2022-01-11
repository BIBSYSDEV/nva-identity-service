package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.core.JsonSerializable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

public interface DynamoEntryWithRangeKey extends WithType, JsonSerializable {

 String FIELD_DELIMITER = "#";


    @DynamoDbPartitionKey
    @DynamoDbAttribute(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY)
     String getPrimaryKeyHashKey();

    /**
     * Setter of the primary hash key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary hash key.
     */
     default void setPrimaryKeyHashKey(String primaryRangeKey){
         //DO NOTHING
     }

    @DynamoDbSortKey
    @DynamoDbAttribute(DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY)
     String getPrimaryKeyRangeKey();

    /**
     * Setter of the primary range key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary range key.
     */
    default void setPrimaryKeyRangeKey(String primaryRangeKey){
        //DO NOTHING
    }


    default boolean primaryHashKeyHasNotBeenSet() {
        return isNull(getPrimaryKeyHashKey());
    }

    default boolean primaryRangeKeyHasNotBeenSet() {
        return isNull(getPrimaryKeyRangeKey());
    }

}