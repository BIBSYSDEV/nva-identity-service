package no.unit.nva.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class GetUserHandlerTest extends HandlerTest {

    private static final String BLANK_STRING = " ";

    private RequestInfo requestInfo;
    private Context context;
    private GetUserHandler getUserHandler;

    @BeforeEach
    public void init() {
        databaseService = createDatabaseServiceUsingLocalStorage();
        getUserHandler = new GetUserHandler(envWithTableName, databaseService);
        context = mock(Context.class);
    }

    @DisplayName("handleRequest returns User object with type \"User\"")
    @Test
    public void handleRequestReturnsUserObjectWithTypeRole()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, IOException {
        insertSampleUserToDatabase();

        ByteArrayOutputStream outputStream = sendGetUserRequestToHandler();

        GatewayResponse<ObjectNode> response = GatewayResponse.fromOutputStream(outputStream);
        ObjectNode bodyObject = response.getBodyObject(ObjectNode.class);

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

    @DisplayName("processInput() handles enccoded path parameters")
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

    private ByteArrayOutputStream sendGetUserRequestToHandler() throws IOException {
        requestInfo = createRequestInfoForGetUser(DEFAULT_USERNAME);
        InputStream inputStream = new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
            .withPathParameters(requestInfo.getPathParameters())
            .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        getUserHandler.handleRequest(inputStream, outputStream, context);
        return outputStream;
    }

    private RequestInfo createRequestInfoForGetUser(String username) {
        RequestInfo reqInfo = new RequestInfo();
        reqInfo.setPathParameters(Collections.singletonMap(GetUserHandler.USERNAME_PATH_PARAMETER, username));
        return reqInfo;
    }
}