package no.unit.nva.database;

import static no.unit.nva.database.Constants.DEFAULT_DYNAMO_CLIENT;
import java.net.URI;
import java.util.List;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessmanagement.internals.UserScanResult;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public interface IdentityService {

    String USERS_AND_ROLES_TABLE = new Environment().readEnv("USERS_AND_ROLES_TABLE");

    @JacocoGenerated
    static IdentityService defaultIdentityService() {
        return new IdentityServiceImpl(DEFAULT_DYNAMO_CLIENT);
    }

    UserDto getUser(UserDto queryObject);

    List<UserDto> listUsers(URI institutionId);

    void addUser(UserDto user);

    void addRole(RoleDto roleDto);

    void updateUser(UserDto user);

    RoleDto getRole(RoleDto input);

    UserScanResult fetchOnePageOfUsers(ScanDatabaseRequestV2 scanRequest);
}
