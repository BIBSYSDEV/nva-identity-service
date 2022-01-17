package no.unit.nva.cognito;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ConstantsTest {

    @Test
    public void testAwsRegion() {
        assertThat(Constants.AWS_REGION, is(notNullValue()));
    }

}
