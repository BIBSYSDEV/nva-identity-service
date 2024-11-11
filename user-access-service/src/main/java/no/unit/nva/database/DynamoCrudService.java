package no.unit.nva.database;


import no.unit.nva.useraccessservice.interfaces.DataAccessClass;
import no.unit.nva.useraccessservice.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

public class DynamoCrudService<T extends DataAccessClass<T>> implements DataAccessService<T> {
    private final DynamoDbTable<T> table;

    @JacocoGenerated
    public DynamoCrudService(String tableName, Class<T> tClass) {
        this(DEFAULT_DYNAMO_CLIENT, tableName, tClass);
    }

    public DynamoCrudService(DynamoDbClient dynamoDbClient, String tableName, Class<T> tClass) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromImmutableClass(tClass));
    }

    /**
     * Persists an item in the database.
     *
     * @param newItem the item to persist.
     * @throws IllegalArgumentException if the item is invalid.
     */
    @Override
    public void persist(T newItem)  {
        T.validateBeforePersist(newItem);
        optionalFetchBy(newItem).ifPresentOrElse(
                oldItem -> table.putItem(oldItem.merge(newItem)),
                () -> table.putItem(newItem)
        );
    }

    /**
     * Fetches an item from the database.
     *
     * @param item the item to fetch.
     * @return the fetched item.
     * @throws IllegalArgumentException if the item doesn't contain valid lookup keys.
     * @throws NotFoundException if the item does not exist.
     */
    @Override
    public T fetch(T item) throws NotFoundException {
        T.validateBeforeFetch(item);
        return optionalFetchBy(item)
                .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    /**
     * Deletes an item from the database.
     *
     * @param item the item to delete.
     * @throws IllegalArgumentException if the item doesn't contain valid lookup keys.
     * @throws NotFoundException if the item does not exist.
     */
    @Override
    public void delete(T item) throws NotFoundException {
        fetch(item);
        table.deleteItem(item);
    }

    private Optional<T> optionalFetchBy(T item) {
        var key = Key.builder()
                .partitionValue(item.id().toString())
                .sortValue(item.type())
                .build();
        var result = table.getItem(requestBuilder -> requestBuilder.key(key));
        return Optional.ofNullable(result);
    }
}

