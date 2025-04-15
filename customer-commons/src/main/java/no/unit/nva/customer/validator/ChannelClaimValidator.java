package no.unit.nva.customer.validator;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class ChannelClaimValidator {

    private static final String API_HOST = "API_HOST";
    private static final String CHANNEL_REQUIRED = "Channel required";
    private static final String INVALID_CHANNEL_MESSAGE = "Invalid channel";
    private static final Environment ENVIRONMENT = new Environment();
    private static final String PUBLICATION_CHANNEL_PATH = ENVIRONMENT.readEnv("PUBLICATION_CHANNEL_PATH");
    private static final String HOST = ENVIRONMENT.readEnv(API_HOST);
    private static final String SERIAL_PUBLICATION = "serial-publication";
    private static final String PUBLISHER = "publisher";
    private static final List<String> CHANNEL_TYPES = List.of(SERIAL_PUBLICATION, PUBLISHER);
    private static final String SLASH = "/";
    private static final String EMPTY_STRING = "";
    private static final int THREE = 3;

    @JacocoGenerated
    public ChannelClaimValidator() {}

    public static void validate(ChannelClaimDto channelClaim) throws BadRequestException {
        if (isNull(channelClaim) || isNull(channelClaim.channel())) {
            throw new BadRequestException(CHANNEL_REQUIRED);
        }
        if (!isValidPublicationChannel(channelClaim.channel())) {
            throw new BadRequestException(INVALID_CHANNEL_MESSAGE);
        }
    }

    private static boolean isValidPublicationChannel(URI channel) {
        return HOST.equals(channel.getHost()) && pathIsValid(channel);
    }

    private static boolean pathIsValid(URI channel) {
        var pathElements = channel.getPath().replaceFirst(SLASH, EMPTY_STRING).split(SLASH);

        if (pathElements.length != THREE) {
            return false;
        }

        return isChannelPath(pathElements[0]) && isChannelType(pathElements[1]) && isUuid(pathElements[2]);
    }

    private static boolean isChannelPath(String string) {
        return PUBLICATION_CHANNEL_PATH.equals(string);
    }

    private static boolean isChannelType(String string) {
        return CHANNEL_TYPES.contains(string);
    }

    private static boolean isUuid(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
