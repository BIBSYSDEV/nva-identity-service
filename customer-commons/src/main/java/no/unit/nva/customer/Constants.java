package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

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
    public static CustomerService defaultCustomerService(DynamoDbClient client) {
        return new DynamoDBCustomerService(client);
    }

    private static String dynamoDbServiceEndpoint() {
        return URI.create(String.format("https://dynamodb.%s.amazonaws.com", AWS_REGION)).toString();
    }

    @JacocoGenerated
    private static DynamoDbClient defaultDynamoDbClient() {
        {
            return DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(AWS_REGION))
                .build();
        }
    }
}
