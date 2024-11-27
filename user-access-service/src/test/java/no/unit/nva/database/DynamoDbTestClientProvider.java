package no.unit.nva.database;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbTestClientProvider {

    public static DynamoDbClient geClient() {
        return  DynamoDBEmbedded.create().dynamoDbClient();
    }

}