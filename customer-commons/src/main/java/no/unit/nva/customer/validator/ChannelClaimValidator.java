package no.unit.nva.customer.validator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.net.URI;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class ChannelClaimValidator {

    private static final String API_HOST = "API_HOST";
    private static final String CHANNEL_REQUIRED = "Channel required";
    private static final String INVALID_CHANNEL_MESSAGE = "Invalid channel";
    private static final Environment ENVIRONMENT = new Environment();
    private static final String PUBLICATION_CHANNEL_PATH = ENVIRONMENT.readEnv("PUBLICATION_CHANNEL_PATH");
    private static final String HOST = ENVIRONMENT.readEnv(API_HOST);
    private static final String TRAILING_YEAR_MESSAGE = "Channel should not have trailing year";
    private static final String REGEX_FOUR_DIGITS = "\\d{4}";

    @JacocoGenerated
    public ChannelClaimValidator() {}

    public static void validate(ChannelClaimDto channelClaim) throws BadRequestException {
        if (isNull(channelClaim) || isNull(channelClaim.channel())) {
            throw new BadRequestException(CHANNEL_REQUIRED);
        }
        if (isNotPublicationChannel(channelClaim.channel())) {
            throw new BadRequestException(INVALID_CHANNEL_MESSAGE);
        }
        if (hasTrailingYear(channelClaim.channel())) {
            throw new BadRequestException(TRAILING_YEAR_MESSAGE);
        }
    }

    private static boolean isNotPublicationChannel(URI channel) {
        return !HOST.equals(channel.getHost()) || !channel.toString().contains(PUBLICATION_CHANNEL_PATH);
    }

    private static boolean hasTrailingYear(URI channel) {
        var lastPathElement = UriWrapper.fromUri(channel).getLastPathElement();
        return nonNull(lastPathElement) && lastPathElement.matches(REGEX_FOUR_DIGITS);
    }
}
