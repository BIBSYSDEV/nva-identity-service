package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.util.Optional;

public final class RequestUtils {

    private RequestUtils() {

    }

    public static Optional<String> getPathParameter(APIGatewayProxyRequestEvent requestInfo, String pathParameter) {
        return Optional.ofNullable(requestInfo)
            .map(APIGatewayProxyRequestEvent::getPathParameters)
            .map(m -> m.get(pathParameter));
    }
}
