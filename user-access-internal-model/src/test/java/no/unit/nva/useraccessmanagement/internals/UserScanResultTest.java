package no.unit.nva.useraccessmanagement.internals;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.List;
import java.util.Map;
import no.unit.nva.useraccessmanagement.model.UserDto;
import org.junit.jupiter.api.Test;

class UserScanResultTest {

    @Test
    void shouldReturnObjectContainingInputArguments() {
        var sampleUsers = List.of(UserDto.newBuilder().withUsername(randomString()).build());
        var marker = Map.of(randomString(), new AttributeValue(randomString()),
                           randomString(), new AttributeValue(randomString()));
        var moreResults= randomBoolean();
        var result = new UserScanResult(sampleUsers,marker,moreResults);
        assertThat(result.getRetrievedUsers(),is(equalTo(sampleUsers)));
        assertThat(result.getStartMarkerForNextScan(),is(equalTo(marker)));
        assertThat(result.thereAreMoreEntries(),is(equalTo(moreResults)));
    }
}