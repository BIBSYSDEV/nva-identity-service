package no.unit.nva.customer.validator;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelConstraintDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class ChannelClaimValidatorTest {
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
}