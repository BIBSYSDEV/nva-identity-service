package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.JsonUtils.objectMapper;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import nva.commons.core.JsonSerializable;

public abstract class DynamoEntryWithRangeKey implements WithType, JsonSerializable {

    public static final TypeFactory TYPE_FACTORY = objectMapper.getTypeFactory();
    private static final Map<String, JavaType> JAVA_TYPES = new ConcurrentHashMap<>();
    @SuppressWarnings("PMD.ConstantsInInterface")
    public static String FIELD_DELIMITER = "#";

    /**
     * Generated DynamoEntry from an {@link Item}.
     *
     * @param item       the item.
     * @param entryClass the class of the object.
     * @param <E>        the type of the object
     * @return an instance of class {@code E}
     */
    public static <E extends DynamoEntryWithRangeKey> E fromItem(Item item, Class<E> entryClass) {
        if (nonNull(item)) {
            JavaType javaType = fetchJavaType(entryClass);
            return objectMapper.convertValue(item.asMap(), javaType);
        }
        return null;
    }

    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY)
    public abstract String getPrimaryHashKey();

    /**
     * Setter of the primary hash key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary hash key.
     * @throws InvalidEntryInternalException when the serialization in invalid.
     */
    public abstract void setPrimaryHashKey(String primaryRangeKey) throws InvalidEntryInternalException;

    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY)
    public abstract String getPrimaryRangeKey();

    /**
     * Setter of the primary range key. This method is supposed to be used only by when deserializing an item using Json
     * serializer.
     *
     * @param primaryRangeKey the primary range key.
     * @throws InvalidEntryInternalException when the serialization in invalid.
     */
    public abstract void setPrimaryRangeKey(String primaryRangeKey) throws InvalidEntryInternalException;

    public Item toItem() {
        String jsonString = this.toJsonString();
        return Item.fromJSON(jsonString);
    }

    protected boolean primaryHashKeyHasNotBeenSet() {
        return isNull(getPrimaryHashKey());
    }

    protected boolean primaryRangeKeyHasNotBeenSet() {
        return isNull(getPrimaryRangeKey());
    }

    private static <E> JavaType fetchJavaType(Class<E> entryClass) {
        return JAVA_TYPES.getOrDefault(entryClass.getName(), constructNewJavaType(entryClass));
    }

    private static <E> JavaType constructNewJavaType(Class<E> entryClass) {
        JavaType javaType = TYPE_FACTORY.constructType(entryClass);
        JAVA_TYPES.put(entryClass.getName(), javaType);
        return javaType;
    }
}