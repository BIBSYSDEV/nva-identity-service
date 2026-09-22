package no.unit.nva.database;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.dynamodb.services.local.embedded.DynamoDBEmbedded;

public final class DatabaseTestConfig {

  private DatabaseTestConfig() {}

  public static DynamoDbClient getEmbeddedClient() {
    return DynamoDBEmbedded.create(null, true).dynamoDbClient();
  }
}
