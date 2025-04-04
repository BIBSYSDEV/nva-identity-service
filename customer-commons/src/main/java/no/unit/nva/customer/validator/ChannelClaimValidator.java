package no.unit.nva.customer.validator;

import static java.util.Objects.isNull;
import java.net.URI;
import no.unit.nva.customer.model.ChannelClaimDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class ChannelClaimValidator {
    private static final String API_HOST = "API_HOST";
    private static final String CHANNEL_REQUIRED = "Channel required";
    private static final String INVALID_CHANNEL_MESSAGE = "%s is not a valid channel";
    private static final String PUBLICATION_CHANNEL_PATH = "PUBLICATION_CHANNEL_PATH";
    private static final Environment ENVIRONMENT = new Environment();

    @JacocoGenerated
    public ChannelClaimValidator() {}

    public static void validate(ChannelClaimDto channelClaim) throws BadRequestException {
        if (isNull(channelClaim) || isNull(channelClaim.channel())) {
            throw new BadRequestException(CHANNEL_REQUIRED);
        }
        if (!isPublicationChannel(channelClaim.channel())) {
            throw new BadRequestException(INVALID_CHANNEL_MESSAGE.formatted(channelClaim.channel()));
        }
    }

    private static boolean isPublicationChannel(URI channelId) {
        var host = ENVIRONMENT.readEnv(API_HOST);
        var publicationChannelPath = ENVIRONMENT.readEnv(PUBLICATION_CHANNEL_PATH);
        return host.equals(channelId.getHost()) && channelId.toString().contains(publicationChannelPath);
    }
}
