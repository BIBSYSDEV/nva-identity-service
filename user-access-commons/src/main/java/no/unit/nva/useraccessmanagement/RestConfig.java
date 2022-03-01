package no.unit.nva.useraccessmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class RestConfig {

    public static final ObjectMapper defaultRestObjectMapper = JsonUtils.dtoObjectMapper;

    private RestConfig() {
    }
}
