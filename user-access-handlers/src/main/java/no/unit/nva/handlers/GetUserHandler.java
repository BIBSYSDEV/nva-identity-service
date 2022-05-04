package no.unit.nva.handlers;

import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Optional;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetUserHandler extends HandlerAccessingUser<Void, UserDto> {

    private final IdentityService databaseService;

    @JacocoGenerated
    public GetUserHandler() {
        this(new IdentityServiceImpl());
    }

    public GetUserHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, NotFoundException {
        String username = extractValidUserNameOrThrowException(requestInfo);
        UserDto queryObject = UserDto.newBuilder().withUsername(username).build();
        return databaseService.getUser(queryObject);
    }

    private String extractValidUserNameOrThrowException(RequestInfo requestInfo) {
        return Optional.of(requestInfo)
            .map(RequestInfo::getPathParameters)
            .map(map -> map.get(USERNAME_PATH_PARAMETER))
            .map(this::decodeUrlPart)
            .filter(not(String::isBlank))
            .orElseThrow();
    }
}
