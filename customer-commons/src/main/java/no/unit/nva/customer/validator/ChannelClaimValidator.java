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
    private static final String INVALID_CHANNEL_MESSAGE = "Invalid channel";
    private static final Environment ENVIRONMENT = new Environment();
    private static final String PUBLICATION_CHANNEL_PATH = ENVIRONMENT.readEnv("PUBLICATION_CHANNEL_PATH");
    private static final String HOST = ENVIRONMENT.readEnv(API_HOST);

    @JacocoGenerated
    public ChannelClaimValidator() {}

    public static void validate(ChannelClaimDto channelClaim) throws BadRequestException {
        if (isNull(channelClaim) || isNull(channelClaim.channel())) {
            throw new BadRequestException(CHANNEL_REQUIRED);
        }
        if (isNotPublicationChannel(channelClaim.channel())) {
            throw new BadRequestException(INVALID_CHANNEL_MESSAGE);
        }
    }

    private static boolean isNotPublicationChannel(URI channelId) {
        return !HOST.equals(channelId.getHost()) || !channelId.toString().contains(PUBLICATION_CHANNEL_PATH);
    }
}
