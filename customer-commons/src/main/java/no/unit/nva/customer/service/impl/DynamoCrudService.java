package no.unit.nva.customer.service.impl;


import no.unit.nva.customer.model.interfaces.DataAccessClass;
import no.unit.nva.customer.model.interfaces.DataAccessService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
    public void persist(T item) throws NotFoundException {
        if (isNull(item.modifiedBy())) {
            throw new IllegalArgumentException("UserId is required for persisting");
        }
        var existingItem = fetch(item);
        if (nonNull(existingItem)) {
            table.putItem(existingItem.merge(item));
        } else {
            table.putItem(item);
        }
    }


    @Override
    public T fetch(T item) throws NotFoundException {
        Key key = Key.builder().partitionValue(item.withId().toString()).sortValue(item.withType()).build();
        return Optional.ofNullable(table.getItem(r -> r.key(key)))
                .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }
}

