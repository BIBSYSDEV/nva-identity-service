package no.unit.nva.useraccessservice.internals;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;
import java.util.Map;

import no.unit.nva.useraccessservice.model.UserDto;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class UserScanResultTest {

    @Test
    void shouldReturnObjectContainingInputArguments() {
        var sampleUsers = List.of(UserDto.newBuilder().withUsername(randomString()).build());
        var marker = Map.of(randomString(), stringAttributeValue(),
                randomString(), stringAttributeValue());
        var moreResults = randomBoolean();
        var result = new UserScanResult(sampleUsers, marker, moreResults);
        assertThat(result.getRetrievedUsers(), is(equalTo(sampleUsers)));
        assertThat(result.getStartMarkerForNextScan(), is(equalTo(marker)));
        assertThat(result.thereAreMoreEntries(), is(equalTo(moreResults)));
    }

    private AttributeValue stringAttributeValue() {
        return AttributeValue.builder().s(randomString()).build();
    }
}