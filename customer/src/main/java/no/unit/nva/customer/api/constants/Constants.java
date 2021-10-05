package no.unit.nva.customer.api.constants;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class Constants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";


    @JacocoGenerated
    public static CustomerService defaultCustomerService() {
        AmazonDynamoDB client = defaultDynamoDbClient();
        return new DynamoDBCustomerService(client, ObjectMapperConfig.objectMapper, ENVIRONMENT);
    }


    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        EndpointConfiguration endpointConfiguration =
            new EndpointConfiguration(no.unit.nva.customer.Constants.AWD_DYNAMODB_SERVICE_END_POINT,
                                      no.unit.nva.customer.Constants.AWS_REGION);
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withEndpointConfiguration(endpointConfiguration)
            .build();
    }

}
