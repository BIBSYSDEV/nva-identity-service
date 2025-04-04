package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.net.URI;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

@JacocoGenerated
public final class Constants {

    public static final String LINKED_DATA_ID = "id";
    public static final String LINKED_DATA_CONTEXT = "@context";
    public static final URI LINKED_DATA_CONTEXT_VALUE =
        URI.create("https://bibsysdev.github.io/src/customer-context.json");

    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    private Constants() {
    }

    @JacocoGenerated
    public static CustomerService defaultCustomerService() {
        return defaultCustomerService(DEFAULT_DYNAMO_CLIENT);
    }

    @JacocoGenerated
    public static CustomerService defaultCustomerService(DynamoDbClient client) {
        return new DynamoDBCustomerService(client);
    }
}
