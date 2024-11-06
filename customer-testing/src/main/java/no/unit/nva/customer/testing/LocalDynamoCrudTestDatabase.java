package no.unit.nva.customer.testing;


import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class LocalDynamoCrudTestDatabase {

    protected static DynamoDbClient dynamoClient;

    public void init(String tableName) {
        dynamoClient = DynamoDbTestClientProvider.geClient();
        new DynamoDbTableCreator(dynamoClient).createTable(tableName);
    }

}