package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import static no.unit.nva.handlers.HandlerAccessingUser.EMPTY_USERNAME_PATH_PARAMETER_ERROR;
import static no.unit.nva.handlers.HandlerAccessingUser.USERNAME_PATH_PARAMETER;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.ApiGatewayHandlerV2;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUserHandler extends ApiGatewayHandlerV2<Void, UserDto> {

    private final IdentityService databaseService;
    private final Logger logger = LoggerFactory.getLogger(ApiGatewayHandlerV2.class);

    @JacocoGenerated
    public GetUserHandler() {
        this(new IdentityServiceImpl());
    }

    public GetUserHandler(IdentityService databaseService) {
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserDto processInput(String body,
                                   APIGatewayProxyRequestEvent input,
                                   Context context) throws ApiGatewayException {
        logger.info("Hello from ApiGatewayHandlerV2");
        logger.info(input.toString());
        String username = extractValidUserNameOrThrowException(input);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        return databaseService.getUser(queryObject);
    }

    protected String decodeUrlPart(String encodedString) {
        return java.net.URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
    }

    private String extractValidUserNameOrThrowException(APIGatewayProxyRequestEvent requestInfo)
        throws BadRequestException {
        return Optional.of(requestInfo)
            .map(APIGatewayProxyRequestEvent::getPathParameters)
            .map(map -> map.get(USERNAME_PATH_PARAMETER))
            .map(this::decodeUrlPart)
            .filter(not(String::isBlank))
            .orElseThrow(() -> new BadRequestException(EMPTY_USERNAME_PATH_PARAMETER_ERROR));
    }
}
