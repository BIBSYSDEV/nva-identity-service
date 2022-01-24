package no.unit.nva.useraccess.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class EventsConfig {

    public static final String IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC = "IdentityService.IdentityEntry.ScanAndUpdate";
    public static final String SCAN_REQUEST_EVENTS_DETAIL_TYPE = "topicInDetailType";
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final String EVENT_BUS = new Environment().readEnv("EVENT_BUS");

    private EventsConfig() {

    }
}
