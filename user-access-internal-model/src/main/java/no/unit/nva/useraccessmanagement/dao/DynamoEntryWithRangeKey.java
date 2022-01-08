package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.core.JsonSerializable;

public abstract class DynamoEntryWithRangeKey implements WithType, JsonSerializable {

    public static String FIELD_DELIMITER = "#";


    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY)
    public abstract String getPrimaryHashKey();

    /**
     * Setter of the primary hash key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary hash key.
     */
    public abstract void setPrimaryHashKey(String primaryRangeKey);

    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY)
    public abstract String getPrimaryRangeKey();

    /**
     * Setter of the primary range key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary range key.
     */
    public abstract void setPrimaryRangeKey(String primaryRangeKey);


    protected boolean primaryHashKeyHasNotBeenSet() {
        return isNull(getPrimaryHashKey());
    }

    protected boolean primaryRangeKeyHasNotBeenSet() {
        return isNull(getPrimaryRangeKey());
    }

}