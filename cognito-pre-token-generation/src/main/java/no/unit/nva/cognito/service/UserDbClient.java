package no.unit.nva.cognito.service;

import no.unit.nva.cognito.exception.UserServiceException;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UserDbClient implements UserApi {

    private static final Logger logger = LoggerFactory.getLogger(UserDbClient.class);
    public static final String ERROR_GETTING_USER = "Error getting user";
    public static final String ERROR_CREATING_USER = "Error creating user";
    public static final String ERROR_UPDATING_USER = "Error updating user";

    private final IdentityService databaseService;

    public UserDbClient(IdentityService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Optional<UserDto> getUser(String username) {
        UserDto userDto = null;
        try {
            UserDto queryOject = UserDto.newBuilder().withUsername(username).build();
            userDto = databaseService.getUser(queryOject);
        } catch (Exception e) {
            logger.error(ERROR_GETTING_USER, e);
        }
        return Optional.ofNullable(userDto);
    }

    @Override
    public UserDto createUser(UserDto user) {
        try {
            databaseService.addUser(user);
        } catch (Exception e) {
            logger.error(ERROR_CREATING_USER, e);
            throw new UserServiceException(e.getMessage(), e);
        }
        //TODO: maybe not necessary to return user here?
        return getUser(user.getUsername()).orElseThrow();
    }

    @Override
    public void updateUser(UserDto user) {
        try {
            databaseService.updateUser(user);
        } catch (Exception e) {
            logger.error(ERROR_UPDATING_USER, e);
            throw new UserServiceException(e.getMessage(), e);
        }
    }
}
