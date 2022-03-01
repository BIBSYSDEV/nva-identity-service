package no.unit.nva.cognito.model;

import java.io.IOException;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;

public class EventTest {

    public static final String SAMPLE_EVENT_JSON = "sample_event.json";

    @Test
    public void inputIsParsedToEventWithOrgNumber() throws IOException {

        String eventJson = IoUtils.stringFromResources(Path.of(SAMPLE_EVENT_JSON));

        Event event = defaultRestObjectMapper.readValue(eventJson, Event.class);

        Assertions.assertNotNull(event.getRequest().getUserAttributes().getOrgNumber());
    }

}
