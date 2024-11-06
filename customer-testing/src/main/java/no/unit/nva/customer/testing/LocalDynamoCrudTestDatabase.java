package no.unit.nva.customer.testing;


import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class LocalDynamoCrudTestDatabase {

    protected static DynamoDbClient client;

    public void init(String tableName) {
        client = DynamoDbTestClientProvider.geClient();
        new DynamoDbTableCreator(client).createTable(tableName);
    }

}