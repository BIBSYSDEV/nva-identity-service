package no.unit.nva.database;

import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import java.net.URI;
import java.util.List;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.internals.UserScanResult;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public interface IdentityService {

    String USERS_AND_ROLES_TABLE = new Environment().readEnv("USERS_AND_ROLES_TABLE");

    @JacocoGenerated
    static IdentityService defaultIdentityService() {
        return defaultIdentityService(DEFAULT_DYNAMO_CLIENT);
    }

    @JacocoGenerated
    static IdentityService defaultIdentityService(DynamoDbClient dynamoDbClient) {
        return new IdentityServiceImpl(dynamoDbClient);
    }

    UserDto getUser(UserDto queryObject);

    List<UserDto> listUsers(URI institutionId);

    UserDto addUser(UserDto user);

    void addRole(RoleDto roleDto);

    void updateUser(UserDto user);

    RoleDto getRole(RoleDto input);

    UserScanResult fetchOnePageOfUsers(ScanDatabaseRequestV2 scanRequest);
}
