package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;

public class CreateUserHandler extends HandlerWithEventualConsistency<UserDto, UserDto> {

    @JacocoGenerated
    public CreateUserHandler() {
        super();
    }

    @Override
    protected Integer getSuccessStatusCode(String input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        var request = CreateUserRequest.fromJson(input);
        return UserDto.newBuilder()
            .withInstitution(request.getCustomerId())
            .build();
    }
}
