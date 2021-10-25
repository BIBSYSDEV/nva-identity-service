package no.unit.nva.handlers.authorizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import nva.commons.core.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("PMD.BooleanGetMethodName")
public class SimpleAuthorizerResponse {

    private boolean isAuthorized;

    public SimpleAuthorizerResponse() {
    }


    public boolean getIsAuthorized() {
        return isAuthorized;
    }

    public void setIsAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    public static SimpleAuthorizerResponse fromOutputStream(ByteArrayOutputStream outputStream)
            throws JsonProcessingException {
        String content = outputStream.toString(StandardCharsets.UTF_8);
        return JsonUtils.dtoObjectMapper.readValue(content, SimpleAuthorizerResponse.class);
    }
}
