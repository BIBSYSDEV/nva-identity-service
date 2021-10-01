package no.unit.nva.customer;

import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;

public final class Constants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final List<MediaType> DEFAULT_RESPONSE_MEDIA_TYPES = List.of(
        MediaType.JSON_UTF_8,
        MediaTypes.APPLICATION_JSON_LD
    );

    public static final String DEFAULT_AWS_REGION = "eu-west-1";
    public static final String AWS_REGION = ENVIRONMENT.readEnvOpt("AWS_REGION").orElse(DEFAULT_AWS_REGION);
    public static final String AWD_DYNAMODB_SERVICE_END_POINT = dynamoDbServiceEndpoint();

    private static String dynamoDbServiceEndpoint() {
        return URI.create(String.format("https://dynamodb.%s.amazonaws.com",AWS_REGION)).toString();
    }

    private Constants() {
    }
}
