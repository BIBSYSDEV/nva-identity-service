package no.unit.nva.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class RestConfig {

    public static final ObjectMapper defaultRestObjectMapper = JsonUtils.dtoObjectMapper;

    private RestConfig() {

    }

}
