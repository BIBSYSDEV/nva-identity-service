package no.unit.nva.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JsonUtils;

public final class ObjectMapperConfig {

    public static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private ObjectMapperConfig() {

    }

}
