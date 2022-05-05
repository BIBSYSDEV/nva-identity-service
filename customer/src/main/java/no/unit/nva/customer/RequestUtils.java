package no.unit.nva.customer;

import java.util.Optional;
import nva.commons.apigateway.RequestInfo;

public final class RequestUtils {

    private RequestUtils() {

    }

    public static Optional<String> getPathParameter(RequestInfo requestInfo, String pathParameter) {
        return Optional.ofNullable(requestInfo)
            .map(RequestInfo::getPathParameters)
            .map(m -> m.get(pathParameter));
    }
}
