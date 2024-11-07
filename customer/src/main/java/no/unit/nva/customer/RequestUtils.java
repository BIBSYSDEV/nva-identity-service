package no.unit.nva.customer;

import nva.commons.apigateway.RequestInfo;

import java.util.Optional;

public final class RequestUtils {

    private RequestUtils() {

    }

    public static Optional<String> getPathParameter(RequestInfo requestInfo, String pathParameter) {
        return Optional.ofNullable(requestInfo)
                .map(RequestInfo::getPathParameters)
                .map(m -> m.get(pathParameter));
    }
}
