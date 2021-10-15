package no.unit.nva.useraccessmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;

@JacocoGenerated
public final class RestConfig {

    public static final ObjectMapper defaultRestObjectMapper = JsonUtils.dtoObjectMapper;

    private RestConfig() {
    }
}
