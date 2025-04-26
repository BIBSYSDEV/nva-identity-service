package no.unit.nva.customer.validator;

import static no.unit.nva.customer.model.PublicationInstanceTypes.ACADEMIC_ARTICLE;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_MASTER;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.EVERYONE;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.OWNER_ONLY;
import static no.unit.nva.customer.testing.CustomerDataGenerator.degreeScopes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannel;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelConstraintDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class ChannelClaimValidatorTest {

    private static final String RANDOM_YEAR = "2025";
    private static final int ZERO = 0;
    private static final String EMPTY_STRING = "";

    @Test
    void shouldNotThrowExceptionWhenChannelClaimIsValid() {
        var validChannelClaim = randomChannelClaimDto();
        assertDoesNotThrow(() -> ChannelClaimValidator.validate(validChannelClaim));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenChannelIsMissing() {
        var channelClaimWithoutChannel = new ChannelClaimDto(null, randomChannelConstraintDto());
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithoutChannel));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenChannelIsInvalid() {
        var channelClaimWithInvalidChannel = new ChannelClaimDto(randomUri(), randomChannelConstraintDto());
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithInvalidChannel));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenChannelHasTrailingYear() {
        var channelWithTrailingYear = UriWrapper.fromUri(randomChannel()).addChild(RANDOM_YEAR).getUri();
        var channelClaimWithInvalidChannel = new ChannelClaimDto(channelWithTrailingYear, randomChannelConstraintDto());
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithInvalidChannel));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenLastPathElementOfChannelIsNotUuid() {
        var channelWithRandomLastPathElement = UriWrapper.fromUri(randomChannel())
                                                   .replacePathElementByIndexFromEnd(ZERO, EMPTY_STRING)
                                                   .getUri();
        var channelClaimWithInvalidChannel = new ChannelClaimDto(channelWithRandomLastPathElement,
                                                                 randomChannelConstraintDto());
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithInvalidChannel));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenNoConstraintIsProvided() {
        var channelClaimWithoutConstraint = new ChannelClaimDto(randomChannel(), null);
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithoutConstraint));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenNoScopeIsProvided() {
        var constraintWithoutScope = new ChannelConstraintDto(OWNER_ONLY, OWNER_ONLY, List.of());
        var channelClaimWithoutScope = new ChannelClaimDto(randomChannel(), constraintWithoutScope);
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithoutScope));
    }

    // Temporary tests while constraints are restricted

    @Test
    void shouldThrowBadRequestExceptionWhenPublishingPolicyIsOwnerOnly() {
        var nonDefaultConstraint = new ChannelConstraintDto(OWNER_ONLY, OWNER_ONLY, degreeScopes());
        var channelClaimWithNotDefaultConstraint = new ChannelClaimDto(randomChannel(), nonDefaultConstraint);
        assertThrows(BadRequestException.class,
                     () -> ChannelClaimValidator.validate(channelClaimWithNotDefaultConstraint));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenEditingPolicyIsEveryone() {
        var nonDefaultConstraint = new ChannelConstraintDto(EVERYONE, EVERYONE, degreeScopes());
        var channelClaimWithNotDefaultConstraint = new ChannelClaimDto(randomChannel(), nonDefaultConstraint);
        assertThrows(BadRequestException.class,
                     () -> ChannelClaimValidator.validate(channelClaimWithNotDefaultConstraint));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenWhenAllScopesAreNotDegree() {
        var nonDefaultConstraint = new ChannelConstraintDto(EVERYONE, OWNER_ONLY,
                                                            List.of(ACADEMIC_ARTICLE, DEGREE_MASTER));
        var channelClaimWithNotDefaultConstraint = new ChannelClaimDto(randomChannel(), nonDefaultConstraint);
        assertThrows(BadRequestException.class,
                     () -> ChannelClaimValidator.validate(channelClaimWithNotDefaultConstraint));
    }
}