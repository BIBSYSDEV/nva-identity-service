package no.unit.nva.database;

import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.internals.UserScanResult;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

public interface IdentityService {

    @JacocoGenerated
    static IdentityService defaultIdentityService() {
        return defaultIdentityService(DEFAULT_DYNAMO_CLIENT);
    }

    @JacocoGenerated
    static IdentityService defaultIdentityService(DynamoDbClient dynamoDbClient) {
        return new IdentityServiceImpl(dynamoDbClient);
    }

    UserDto getUser(UserDto queryObject) throws NotFoundException;

    List<UserDto> listAllUsers();

    List<UserDto> listUsers(URI institutionId);

    UserDto addUser(UserDto user) throws ConflictException;

    void addRole(RoleDto roleDto) throws ConflictException, InvalidInputException;

    void updateRole(RoleDto roleDto) throws InvalidInputException, NotFoundException;

    UserDto updateUser(UserDto user) throws NotFoundException;

    RoleDto getRole(RoleDto input) throws NotFoundException;

    UserScanResult fetchOnePageOfUsers(ScanDatabaseRequestV2 scanRequest);

    List<UserDto> getUsersByCristinId(URI cristinPersonId);

    UserDto getUserByPersonCristinIdAndCustomerCristinId(URI cristinPersonId, URI cristinOrgId);

    void addExternalClient(ClientDto clientDto);

    ClientDto getClient(ClientDto queryObject) throws NotFoundException;

    class Constants {

        public static final String USERS_AND_ROLES_TABLE =
            new Environment().readEnv("USERS_AND_ROLES_TABLE");

        private Constants() {

        }
    }
}
