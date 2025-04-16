package no.unit.nva.customer.validator;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannel;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelConstraintDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
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
        var channelClaimWithInvalidChannel = new ChannelClaimDto(channelWithRandomLastPathElement, randomChannelConstraintDto());
        assertThrows(BadRequestException.class, () -> ChannelClaimValidator.validate(channelClaimWithInvalidChannel));
    }
}