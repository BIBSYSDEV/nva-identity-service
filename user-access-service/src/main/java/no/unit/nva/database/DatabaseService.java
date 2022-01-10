package no.unit.nva.database;

import java.util.List;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;

public interface DatabaseService {

    String USERS_AND_ROLES_TABLE_NAME_ENV_VARIABLE = "USERS_AND_ROLES_TABLE";
    String USERS_AND_ROLES_TABLE_NAME = new Environment()
        .readEnvOpt(USERS_AND_ROLES_TABLE_NAME_ENV_VARIABLE).orElse("UndefinedTable");

    UserDto getUser(UserDto queryObject) throws  NotFoundException, InvalidInputException;

    List<UserDto> listUsers(String institutionId);

    void addUser(UserDto user) throws ConflictException, InvalidInputException;

    void addRole(RoleDto roleDto)throws ConflictException, InvalidInputException;

    void updateUser(UserDto user)throws NotFoundException, InvalidInputException;

    RoleDto getRole(RoleDto input) throws  NotFoundException;
}
