package no.unit.nva.handlers;

import java.nio.charset.StandardCharsets;
import nva.commons.apigateway.ApiGatewayHandler;

public abstract class HandlerAccessingUser<I, O> extends ApiGatewayHandler<I, O> {

    public static final String USERS_RELATIVE_PATH = "/users/";
    public static final String USERNAME_PATH_PARAMETER = "username";

    public static final String EMPTY_USERNAME_PATH_PARAMETER_ERROR =
        "Path parameter \"" + USERNAME_PATH_PARAMETER + "\" cannot be empty";

    public HandlerAccessingUser(Class<I> iclass) {
        super(iclass);
    }

    protected String decodeUrlPart(String encodedString) {
        return java.net.URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
    }
}
