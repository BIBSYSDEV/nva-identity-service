package no.unit.nva.useraccessservice.usercreation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import org.junit.jupiter.api.Test;

public class NationalIdentityNumberTest {

    @Test
    void shouldMaskEntireNinIfShorterThanElevenCharacters() {
        var nin = new NationalIdentityNumber("0123456789");

        assertThat("Should mask the entire national identity number in toString() if it is shorter than "
                   + " 11 characters (probably test data).",
                   nin.toString(),
                   containsString("nin='XXXX'"));
    }

    @Test
    void shouldMaskAllButFirstTwoAndLastTwoCharactersIfLongerThanOrEqualToElevenCharacters() {
        var nin = new NationalIdentityNumber("01234567891");

        assertThat("Should mask all but the first two and last two characters in toString() if"
                   + " it is longer than or equal to 11 characters.",
                   nin.toString(),
                   containsString("nin='01XXXX91'"));
    }
}
