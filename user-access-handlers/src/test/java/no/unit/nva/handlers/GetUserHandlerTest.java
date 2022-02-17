package no.unit.nva.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class GetUserHandlerTest extends HandlerTest {

    private static final String BLANK_STRING = " ";
    private APIGatewayProxyRequestEvent requestInfo;
    private Context context;
    private GetUserHandler getUserHandler;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getUserHandler = new GetUserHandler(databaseService);
        context = mock(Context.class);
    }

    @DisplayName("handleRequest returns User object with type \"User\"")
    @Test
    public void handleRequestReturnsUserObjectWithTypeRole()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, IOException {
        insertSampleUserToDatabase();

        requestInfo = createRequestInfoForGetUser(DEFAULT_USERNAME);
        var response = getUserHandler.handleRequest(requestInfo, context);
        var bodyObject = (ObjectNode) JsonUtils.dtoObjectMapper.readTree(response.getBody());

        assertThat(bodyObject.get(TYPE_ATTRIBUTE), is(not(nullValue())));
        String type = bodyObject.get(TYPE_ATTRIBUTE).asText();
        assertThat(type, is(equalTo(UserDto.TYPE)));
    }

    @Test
    void getSuccessStatusCodeReturnsOK() {
        Integer actual = getUserHandler.getSuccessStatusCode(null, null);
        assertThat(actual, is(equalTo(HttpStatus.SC_OK)));
    }

    @DisplayName("processInput() returns UserDto when path parameter contains the username of an existing user")
    @Test
    void processInputReturnsUserDtoWhenPathParameterContainsTheUsernameOfExistingUser() throws ApiGatewayException {
        requestInfo = createRequestInfoForGetUser(DEFAULT_USERNAME);
        UserDto expected = insertSampleUserToDatabase();
        UserDto actual = getUserHandler.processInput(null, requestInfo, context);

        assertThat(actual, is(equalTo(expected)));
    }

    @DisplayName("processInput() handles encoded path parameters")
    @Test
    void processInputReturnsUserDtoWhenPathParameterContainsTheUsernameOfExistingUserEnc() throws ApiGatewayException {

        String encodedUserName = encodeString(DEFAULT_USERNAME);
        requestInfo = createRequestInfoForGetUser(encodedUserName);
        UserDto expected = insertSampleUserToDatabase();
        UserDto actual = getUserHandler.processInput(null, requestInfo, context);
        assertThat(actual, is(equalTo(expected)));
    }

    @DisplayName("processInput() throws NotFoundException when path parameter is a string that is not an existing "
                 + "username")
    @Test
    void processInputThrowsNotFoundExceptionWhenPathParameterIsNonExistingUsername() {

        requestInfo = createRequestInfoForGetUser(DEFAULT_USERNAME);
        Executable action = () -> getUserHandler.processInput(null, requestInfo, context);
        assertThrows(NotFoundException.class, action);
    }

    @DisplayName("processInput() throws BadRequestException when path parameter is a blank string")
    @Test
    void processInputThrowBadRequestExceptionWhenPathParameterIsBlank() {
        requestInfo = createRequestInfoForGetUser(BLANK_STRING);
        Executable action = () -> getUserHandler.processInput(null, requestInfo, context);
        assertThrows(BadRequestException.class, action);
    }

    @DisplayName("processInput() throws BadRequestException when path parameter is null")
    @Test
    void processInputThrowBadRequestExceptionWhenPathParameterIsNull() {
        requestInfo = createRequestInfoForGetUser(null);
        Executable action = () -> getUserHandler.processInput(null, requestInfo, context);
        assertThrows(BadRequestException.class, action);
    }

    private APIGatewayProxyRequestEvent createRequestInfoForGetUser(String username) {
        APIGatewayProxyRequestEvent reqInfo = new APIGatewayProxyRequestEvent();
        reqInfo.setPathParameters(Collections.singletonMap(HandlerAccessingUser.USERNAME_PATH_PARAMETER, username));
        return reqInfo;
    }
}