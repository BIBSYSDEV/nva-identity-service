package no.unit.nva.customer;

import static no.unit.nva.customer.JsonConfig.defaultDynamoConfigMapper;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class Constants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    public static final String DEFAULT_AWS_REGION = "eu-west-1";
    public static final String AWS_REGION = ENVIRONMENT.readEnvOpt("AWS_REGION").orElse(DEFAULT_AWS_REGION);
    public static final String AWD_DYNAMODB_SERVICE_END_POINT = dynamoDbServiceEndpoint();

    private Constants() {
    }


    @JacocoGenerated
    public static CustomerService defaultCustomerService() {
        return defaultCustomerService(defaultDynamoDbClient());
    }

    @JacocoGenerated
    public static CustomerService defaultCustomerService(AmazonDynamoDB client) {
        return new DynamoDBCustomerService(client, defaultDynamoConfigMapper, ENVIRONMENT);
    }

    private static String dynamoDbServiceEndpoint() {
        return URI.create(String.format("https://dynamodb.%s.amazonaws.com", AWS_REGION)).toString();
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        EndpointConfiguration endpointConfiguration =
            new EndpointConfiguration(AWD_DYNAMODB_SERVICE_END_POINT,
                                      AWS_REGION);
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withEndpointConfiguration(endpointConfiguration)
            .build();
    }
}
