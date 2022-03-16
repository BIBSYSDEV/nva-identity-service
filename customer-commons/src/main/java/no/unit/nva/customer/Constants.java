package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.util.List;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.useraccessservice.database.DatabaseConfig;
import nva.commons.apigatewayv2.MediaTypes;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@JacocoGenerated
public final class Constants {

    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    private Constants() {
    }

    @JacocoGenerated
    public static CustomerService defaultCustomerService() {
        return defaultCustomerService(DatabaseConfig.DEFAULT_DYNAMO_CLIENT);
    }

    @JacocoGenerated
    public static CustomerService defaultCustomerService(DynamoDbClient client) {
        return new DynamoDBCustomerService(client);
    }
}
