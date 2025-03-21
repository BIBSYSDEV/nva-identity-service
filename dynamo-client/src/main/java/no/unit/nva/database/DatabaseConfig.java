package no.unit.nva.database;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class DatabaseConfig {

    @JacocoGenerated
    public static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    @JacocoGenerated
    public static final DynamoDbClient DEFAULT_DYNAMO_CLIENT = defaultDynamoDbClient();

    private DatabaseConfig() {
        // NO-OP
    }

    @JacocoGenerated
    private static DynamoDbClient defaultDynamoDbClient() {
        return DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.of(AWS_REGION))
            .build();
    }
}