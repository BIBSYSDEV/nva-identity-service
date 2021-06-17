package no.unit.nva.cognito.service;

import no.unit.nva.cognito.exception.BadGatewayException;
import no.unit.nva.database.DatabaseService;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UserDbClient implements UserApi {

    private static final Logger logger = LoggerFactory.getLogger(UserDbClient.class);
    public static final String ERROR_GETTING_USER = "Error getting user";
    public static final String ERROR_CREATING_USER = "Error creating user";
    public static final String ERROR_UPDATING_USER = "Error updating user";

    private final DatabaseService databaseService;

    public UserDbClient(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Optional<UserDto> getUser(String username) {
        UserDto userDto = null;
        try {
            UserDto queryOject = UserDto.newBuilder().withUsername(username).build();
            userDto = databaseService.getUser(queryOject);
        } catch (InvalidEntryInternalException | NotFoundException e) {
            logger.error(ERROR_GETTING_USER, e);
        }
        return Optional.ofNullable(userDto);
    }

    @Override
    public UserDto createUser(UserDto user) {
        try {
            databaseService.addUser(user);
        } catch (InvalidEntryInternalException | ConflictException | InvalidInputException e) {
            logger.error(ERROR_CREATING_USER, e);
            throw new BadGatewayException(e.getMessage(), e);
        }
        return getUser(user.getUsername()).orElseThrow();
    }

    @Override
    public void updateUser(UserDto user) {
        try {
            databaseService.updateUser(user);
        } catch (InvalidEntryInternalException | InvalidInputException | NotFoundException e) {
            logger.error(ERROR_UPDATING_USER, e);
            throw new BadGatewayException(e.getMessage(), e);
        }
    }
}
