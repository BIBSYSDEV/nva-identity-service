package no.unit.nva.customer.model;

import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class VocabularyStatusTest {

    @Test
    void shouldBeDeserialized() throws IOException {
        var status = objectMapper.beanFrom(VocabularyStatus.class, "\"Allowed\"");
    }
}