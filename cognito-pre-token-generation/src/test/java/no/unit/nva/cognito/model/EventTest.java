package no.unit.nva.cognito.model;

import java.io.IOException;
import java.nio.file.Path;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventTest {

    public static final String SAMPLE_EVENT_JSON = "sample_event.json";

    @Test
    public void inputIsParsedToEventWithOrgNumber() throws IOException {

        String eventJson = IoUtils.stringFromResources(Path.of(SAMPLE_EVENT_JSON));

        Event event = JsonUtils.objectMapper.readValue(eventJson, Event.class);

        Assertions.assertNotNull(event.getRequest().getUserAttributes().getOrgNumber());
    }

}
