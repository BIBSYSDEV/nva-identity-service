package no.unit.nva.handlers;

import java.nio.charset.StandardCharsets;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;

public abstract class HandlerAccessingUser<I, O> extends ApiGatewayHandlerV2<I, O> {

    public static final String USERS_RELATIVE_PATH = "/users/";
    public static final String USERNAME_PATH_PARAMETER = "username";

    public static final String EMPTY_USERNAME_PATH_PARAMETER_ERROR =
        "Path parameter \"" + USERNAME_PATH_PARAMETER + "\" cannot be empty";

    protected HandlerAccessingUser() {
        super();
    }

    protected String decodeUrlPart(String encodedString) {
        return java.net.URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
    }
}
