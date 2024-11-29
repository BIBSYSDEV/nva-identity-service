package no.unit.nva.customer.model;

import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class VocabularyStatusTest {

    @Test
    void shouldBeDeserialized() throws IOException {
        var status = JsonConfig.readValue("\"Allowed\"", VocabularyStatus.class);
    }
}