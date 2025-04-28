package no.unit.nva.customer.validator;

import static no.unit.nva.customer.model.PublicationInstanceTypes.ARTISTIC_DEGREE_PHD;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_BACHELOR;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_LICENTIATE;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_MASTER;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_PHD;
import static no.unit.nva.customer.model.PublicationInstanceTypes.OTHER_STUDENT_WORK;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.EVERYONE;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.OWNER_ONLY;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public final class ChannelClaimValidator {

    private static final String API_HOST = "API_HOST";
    private static final String CHANNEL_REQUIRED = "Channel required";
    private static final String CONSTRAINT_REQUIRED = "Constraint required";
    private static final String SCOPE_REQUIRED = "Scope required";
    private static final String INVALID_CHANNEL_MESSAGE = "Invalid channel";
    private static final String PROVIDED_CONSTRAINT_IS_NOT_ALLOWED = "Provided constraint is not allowed";
    private static final Environment ENVIRONMENT = new Environment();
    private static final String PUBLICATION_CHANNEL_PATH = ENVIRONMENT.readEnv("PUBLICATION_CHANNEL_PATH");
    private static final String HOST = ENVIRONMENT.readEnv(API_HOST);
    private static final String SERIAL_PUBLICATION = "serial-publication";
    private static final String PUBLISHER = "publisher";
    private static final List<String> CHANNEL_TYPES = List.of(SERIAL_PUBLICATION, PUBLISHER);
    private static final String SLASH = "/";
    private static final String EMPTY_STRING = "";
    private static final int THREE = 3;
    private static final List<PublicationInstanceTypes> DEGREES = List.of(DEGREE_BACHELOR, DEGREE_MASTER, DEGREE_PHD,
                                                                          DEGREE_LICENTIATE, ARTISTIC_DEGREE_PHD,
                                                                          OTHER_STUDENT_WORK);

    @JacocoGenerated
    public ChannelClaimValidator() {
    }

    public static void validate(ChannelClaimDto channelClaim) throws BadRequestException {
        if (Optional.ofNullable(channelClaim).map(ChannelClaimDto::channel).isEmpty()) {
            throw new BadRequestException(CHANNEL_REQUIRED);
        }
        if (Optional.of(channelClaim).map(ChannelClaimDto::constraint).isEmpty()) {
            throw new BadRequestException(CONSTRAINT_REQUIRED);
        }
        if (channelClaim.constraint().scope().isEmpty()) {
            throw new BadRequestException(SCOPE_REQUIRED);
        }
        if (!isValidPublicationChannel(channelClaim.channel())) {
            throw new BadRequestException(INVALID_CHANNEL_MESSAGE);
        }
        if (!isDefaultConstraint(channelClaim.constraint())) {
            throw new BadRequestException(PROVIDED_CONSTRAINT_IS_NOT_ALLOWED);
        }
    }

    private static boolean isValidPublicationChannel(URI channel) {
        return HOST.equals(channel.getHost()) && isValidPath(channel);
    }

    private static boolean isValidPath(URI channel) {
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
        return attempt(() -> UUID.fromString(string)).isSuccess();
    }

    // Temporary validation while constraints are restricted
    private static boolean isDefaultConstraint(ChannelConstraintDto constraint) {
        return EVERYONE.equals(constraint.publishingPolicy())
               && OWNER_ONLY.equals(constraint.editingPolicy())
               && new HashSet<>(DEGREES).containsAll(constraint.scope());
    }
}
