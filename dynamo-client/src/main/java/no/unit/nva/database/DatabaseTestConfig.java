package no.unit.nva.database;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DatabaseTestConfig {

    public static DynamoDbClient getEmbeddedClient() {
        return  DynamoDBEmbedded.create().dynamoDbClient();
    }

}