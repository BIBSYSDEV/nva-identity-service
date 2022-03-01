package no.unit.nva.database;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@JacocoGenerated
public final class Constants {

    @JacocoGenerated
    private static final Environment ENVIRONMENT = new Environment();
    @JacocoGenerated
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");

    @JacocoGenerated
    public static final DynamoDbClient DEFAULT_DYNAMO_CLIENT = defaultDynamoDbClient();

    private Constants() {

    }

    @JacocoGenerated
    private static DynamoDbClient defaultDynamoDbClient() {
        return DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(AWS_REGION))
            .build();
    }
}
