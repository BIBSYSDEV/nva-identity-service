package no.unit.nva.database;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.internals.UserScanResult;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

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

  /** Fetches a user by username with a strongly consistent read. */
  Optional<UserDto> fetchUserConsistently(String username);

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

  /** Writes or overwrites the link from a Cognito user to a person. */
  void saveLink(IdentityLink link);

  /** Reads the link for a Cognito username with a strongly consistent read. */
  Optional<IdentityLink> fetchLink(String username);

  /** Writes or overwrites what is known about a person. */
  void saveLinkedPerson(LinkedPerson person);

  /** Reads what is known about a person with a strongly consistent read. */
  Optional<LinkedPerson> fetchLinkedPerson(URI cristinId);

  final class Constants {

    public static final String USERS_AND_ROLES_TABLE =
        new Environment().readEnv("USERS_AND_ROLES_TABLE");

    private Constants() {}
  }
}
