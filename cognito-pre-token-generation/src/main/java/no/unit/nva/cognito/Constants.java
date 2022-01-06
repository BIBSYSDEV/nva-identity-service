package no.unit.nva.cognito;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class Constants {

    public static final Environment ENVIRONMENT = new Environment();

    public static final String AWS_REGION_ENV = "AWS_REGION";
    public static final Regions AWS_REGION_VALUE = setupRegion();
    public static final AmazonDynamoDB DYNAMODB_CLIENT = setupDynamoDBClient();

    private Constants() {
    }

    @JacocoGenerated
    private static AmazonDynamoDB setupDynamoDBClient() {
        return AmazonDynamoDBClientBuilder
            .standard()
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .withRegion(AWS_REGION_VALUE)
            .build();
    }

    @JacocoGenerated
    private static Regions setupRegion() {
        return ENVIRONMENT.readEnvOpt(AWS_REGION_ENV).map(Regions::fromName).orElse(Regions.EU_WEST_1);
    }
}
