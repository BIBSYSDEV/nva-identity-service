package no.unit.nva.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class JsonConfig {

    public static final ObjectMapper defaultDynamoConfigMapper = JsonUtils.dynamoObjectMapper;

    private JsonConfig() {

    }

}
