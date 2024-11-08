package no.unit.nva.database;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class DynamoDbTableCreator {

    public static final String PRIMARY_KEY = "id";
    public static final String SECONDARY_KEY = "type";
    private final DynamoDbClient dynamoDbClient;


    public DynamoDbTableCreator(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void createTable(String tableName) {
        CreateTableRequest request = CreateTableRequest.builder()
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(PRIMARY_KEY)
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName(SECONDARY_KEY)
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(PRIMARY_KEY)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(SECONDARY_KEY)
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .provisionedThroughput(
                        ProvisionedThroughput.builder()
                                .readCapacityUnits(10L)
                                .writeCapacityUnits(10L)
                                .build()
                )
                .tableName(tableName)
                .build();

        dynamoDbClient.createTable(request);
        System.out.println("Table " + tableName + " created successfully!");
    }
}