package no.unit.nva.useraccess.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class EventsConfig {

    public static final String IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC = "IdentityService.IdentityEntry.ScanAndUpdate";
    public static final ObjectMapper objectMapper= JsonUtils.dtoObjectMapper;

    private EventsConfig() {

    }
}
