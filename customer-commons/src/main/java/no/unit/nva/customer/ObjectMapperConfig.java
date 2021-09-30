package no.unit.nva.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class ObjectMapperConfig {

    public static final ObjectMapper objectMapper = JsonUtils.objectMapperWithEmpty;

    private ObjectMapperConfig() {

    }

}
