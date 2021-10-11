package no.unit.nva.useraccessmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class RestConfig {

    public static final ObjectMapper defaultRestObjectMapper = JsonUtils.dtoObjectMapper;

}
