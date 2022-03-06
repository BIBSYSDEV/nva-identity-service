package no.unit.nva.customer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ConstantsTest {

    @Test
    public void defaultResponseMediaTypesIsAList() {
        assertThat(Constants.DEFAULT_RESPONSE_MEDIA_TYPES, notNullValue());
        assertThat(Constants.DEFAULT_RESPONSE_MEDIA_TYPES, is(instanceOf(List.class)));
    }

}
