package no.unit.nva.useraccessmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class DynamoConfig {

    public static final ObjectMapper defaultDynamoConfigMapper = JsonUtils.dynamoObjectMapper;

    private DynamoConfig() {
        
    }

}
