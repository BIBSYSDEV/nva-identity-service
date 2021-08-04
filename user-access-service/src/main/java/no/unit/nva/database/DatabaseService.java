package no.unit.nva.database;

import java.util.List;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

public interface DatabaseService {

    String USERS_AND_ROLES_TABLE_NAME_ENV_VARIABLE = "USERS_AND_ROLES_TABLE";

    UserDto getUser(UserDto queryObject) throws InvalidEntryInternalException, NotFoundException;

    List<UserDto> listUsers(String institutionId) throws InvalidEntryInternalException;

    void addUser(UserDto user) throws InvalidEntryInternalException, ConflictException, InvalidInputException;

    void addRole(RoleDto roleDto)
        throws ConflictException, InvalidInputException, InvalidEntryInternalException;

    void updateUser(UserDto user)
        throws InvalidEntryInternalException, NotFoundException, InvalidInputException;

    RoleDto getRole(RoleDto input) throws InvalidEntryInternalException, NotFoundException;
}
