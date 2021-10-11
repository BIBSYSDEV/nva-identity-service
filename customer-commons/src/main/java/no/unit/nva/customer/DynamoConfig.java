package no.unit.nva.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class DynamoConfig {

    public static final ObjectMapper defaultDynamoConfigMapper = JsonUtils.dynamoObjectMapper;

    private DynamoConfig() {

    }

}
