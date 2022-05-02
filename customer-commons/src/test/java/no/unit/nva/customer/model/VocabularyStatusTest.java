package no.unit.nva.customer.model;

import java.io.IOException;
import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;

class VocabularyStatusTest {

    @Test
    void shouldBeDeserialized() throws IOException {
        var status = JsonConfig.readValue("\"Allowed\"", VocabularyStatus.class);
    }
}