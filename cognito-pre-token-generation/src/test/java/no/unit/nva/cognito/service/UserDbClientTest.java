package no.unit.nva.cognito.service;

import no.unit.nva.cognito.exception.UserServiceException;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class UserDbClientTest {

    public static final String SAMPLE_USERNAME = "username";
    private UserApi userApi;
    private DatabaseService databaseService;

    @BeforeEach
    public void setUp() {
        databaseService = Mockito.mock(DatabaseService.class);
        userApi = new UserDbClient(databaseService);
    }

    @Test
    public void getUserReturnsUserDtoWhenUserExists() throws InvalidEntryInternalException, NotFoundException {
        UserDto userDto = UserDto.newBuilder().withUsername(SAMPLE_USERNAME).build();
        when(databaseService.getUser(any())).thenReturn(userDto);

        Optional<UserDto> user = userApi.getUser(SAMPLE_USERNAME);

        assertThat(user.isPresent(), is(true));
        assertThat(user.get(), is(userDto));
    }

    @Test
    public void getUserReturnsOptionalEmptyWhenUserIsNotFound()
            throws InvalidEntryInternalException, NotFoundException {
        when(databaseService.getUser(any())).thenThrow(NotFoundException.class);

        Optional<UserDto> user = userApi.getUser(SAMPLE_USERNAME);

        assertThat(user.isPresent(), is(false));
    }

    @Test
    public void createUserReturnsCreatedUserOnSuccess()
            throws InvalidEntryInternalException, NotFoundException {
        UserDto userDto = UserDto.newBuilder().withUsername(SAMPLE_USERNAME).build();
        when(databaseService.getUser(any())).thenReturn(userDto);

        UserDto user = userApi.createUser(userDto);

        assertThat(user, is(notNullValue()));
        assertThat(user, is(userDto));
    }

    @Test
    public void createUserThrowsBadGatewayExceptionOnFailure()
            throws InvalidEntryInternalException, InvalidInputException, ConflictException {
        UserDto userDto = UserDto.newBuilder().withUsername(SAMPLE_USERNAME).build();
        doThrow(InvalidInputException.class).when(databaseService).addUser(any());

        Exception exception = Assert.assertThrows(UserServiceException.class, () -> userApi.createUser(userDto));

        assertThat(exception, instanceOf(UserServiceException.class));
    }

    @Test
    public  void updateUserSuccess() throws InvalidEntryInternalException {
        UserDto userDto = UserDto.newBuilder().withUsername(SAMPLE_USERNAME).build();
        userApi.updateUser(userDto);
    }

    @Test
    public void updateUserFailure()
            throws InvalidEntryInternalException, InvalidInputException, NotFoundException {
        UserDto userDto = UserDto.newBuilder().withUsername(SAMPLE_USERNAME).build();
        doThrow(InvalidInputException.class).when(databaseService).updateUser(any());

        Exception exception = Assert.assertThrows(UserServiceException.class, () -> userApi.updateUser(userDto));

        assertThat(exception, instanceOf(UserServiceException.class));
    }

}
