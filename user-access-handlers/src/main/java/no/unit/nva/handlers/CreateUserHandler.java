package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class CreateUserHandler extends HandlerWithEventualConsistency<CreateUserRequest, UserDto> {

    @JacocoGenerated
    public CreateUserHandler() {
        super(CreateUserRequest.class);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUserRequest input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected UserDto processInput(CreateUserRequest input, RequestInfo requestInfo, Context context)
        throws ForbiddenException {
        authorize(requestInfo);
        return UserDto.newBuilder()
            .withInstitution(input.getCustomerId())
            .build();
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if(!requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_USERS.toString())){
            throw new ForbiddenException();
        }


    }
}
