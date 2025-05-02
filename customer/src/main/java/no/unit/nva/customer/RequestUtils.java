package no.unit.nva.customer;

import static nva.commons.core.attempt.Try.attempt;
import java.util.UUID;
import nva.commons.apigateway.RequestInfo;

import java.util.Optional;
import nva.commons.apigateway.exceptions.BadRequestException;

public final class RequestUtils {

    private static final String IDENTIFIER = "identifier";
    private static final String CHANNEL_CLAIM_IDENTIFIER = "channelClaimIdentifier";
    private static final String INVALID_IDENTIFIER_MESSAGE = "Invalid identifier in path provided!";

    private RequestUtils() {

    }

    public static Optional<String> getPathParameter(RequestInfo requestInfo, String pathParameter) {
        return Optional.ofNullable(requestInfo)
            .map(RequestInfo::getPathParameters)
            .map(m -> m.get(pathParameter));
    }

    public static UUID getIdentifier(RequestInfo requestInfo) throws BadRequestException {
        return attempt(() -> requestInfo.getPathParameter(IDENTIFIER))
                   .map(UUID::fromString)
                   .orElseThrow(failure -> new BadRequestException(INVALID_IDENTIFIER_MESSAGE));
    }

    public static UUID getChannelClaimIdentifier(RequestInfo requestInfo) throws BadRequestException {
        return attempt(() -> requestInfo.getPathParameter(CHANNEL_CLAIM_IDENTIFIER))
                   .map(UUID::fromString)
                   .orElseThrow(failure -> new BadRequestException(INVALID_IDENTIFIER_MESSAGE));
    }
}
