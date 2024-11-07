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
    private final DynamoDbEnhancedClient enhancedClient;

    @JacocoGenerated
    public DynamoCrudService(String tableName, Class<T> tClass) {
        this(DEFAULT_DYNAMO_CLIENT, tableName, tClass);
    }

    public DynamoCrudService(DynamoDbClient dynamoDbClient, String tableName, Class<T> tClass) {
        enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromImmutableClass(tClass));
    }

    @Override
    public void persist(T newItem) throws IllegalArgumentException {

        T.validate(newItem);

        optionalFetchBy(newItem).ifPresentOrElse(
                oldItem -> table.putItem(oldItem.merge(newItem)),
                () -> table.putItem(newItem)
        );
    }

    @Override
    public T fetch(T item) throws NotFoundException {
        return optionalFetchBy(item).orElseThrow(() -> new NotFoundException(DataAccessService.RESOURCE_NOT_FOUND_MESSAGE));
    }

    private Optional<T> optionalFetchBy(T item) {
        var key = Key.builder()
                .partitionValue(item.id().toString())
                .sortValue(item.type())
                .build();
        return Optional.ofNullable(table.getItem(r -> r.key(key)));
    }
}

