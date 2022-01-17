package no.unit.nva.cognito;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class Constants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Region AWS_REGION = setupRegion();
    public static final DynamoDbClient DYNAMODB_CLIENT = setupDynamoDBClient();

    private Constants() {
    }

    @JacocoGenerated
    private static DynamoDbClient setupDynamoDBClient() {
        return DynamoDbClient
            .builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    @JacocoGenerated
    private static Region setupRegion() {
        return ENVIRONMENT.readEnvOpt("AWS_REGION").map(Region::of).orElse(Region.EU_WEST_1);
    }
}
