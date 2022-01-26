package no.unit.nva.database;

import java.net.URI;
import java.util.List;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.internals.UserScanResult;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;

public interface IdentityService {

    String USERS_AND_ROLES_TABLE = new Environment().readEnv("USERS_AND_ROLES_TABLE");

    UserDto getUser(UserDto queryObject) throws  NotFoundException, InvalidInputException;

    List<UserDto> listUsers(URI institutionId);

    void addUser(UserDto user) throws ConflictException, InvalidInputException;

    void addRole(RoleDto roleDto)throws ConflictException, InvalidInputException;

    void updateUser(UserDto user)throws NotFoundException, InvalidInputException;

    RoleDto getRole(RoleDto input) throws  NotFoundException;

    UserScanResult fetchOnePageOfUsers(ScanDatabaseRequest scanRequest);
}
