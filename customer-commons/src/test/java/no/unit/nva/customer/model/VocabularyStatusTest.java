package no.unit.nva.customer.model;

import java.io.IOException;
import no.unit.nva.customer.RestConfig;
import org.junit.jupiter.api.Test;

class VocabularyStatusTest {

    @Test
    void shouldBeDeserialized() throws IOException {
        var status = RestConfig.defaultRestObjectMapper.beanFrom(VocabularyStatus.class, "\"Allowed\"");
    }
}