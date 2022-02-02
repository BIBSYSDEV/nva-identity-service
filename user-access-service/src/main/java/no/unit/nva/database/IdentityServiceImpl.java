package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.database.Constants.DEFAULT_DYNAMO_CLIENT;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessmanagement.dao.UserDao;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.internals.UserScanResult;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class IdentityServiceImpl implements IdentityService {

    private final UserService userService;
    private final RoleService roleService;
    private final DynamoDbClient dynamoDbClient;

    @JacocoGenerated
    public IdentityServiceImpl() {
        this(DEFAULT_DYNAMO_CLIENT);
    }

    public IdentityServiceImpl(DynamoDbClient dynamoDbClient) {
        super();
        this.roleService = new RoleService(dynamoDbClient);
        this.userService = new UserService(dynamoDbClient, roleService);
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public UserDto getUser(UserDto queryObject)
        throws NotFoundException, InvalidInputException {
        return userService.getUser(queryObject);
    }

    @Override
    public List<UserDto> listUsers(URI institutionId) {
        return userService.listUsers(institutionId);
    }

    @Override
    public void addUser(UserDto user) throws ConflictException {
        this.userService.addUser(user);
    }

    @Override
    public void addRole(RoleDto roleDto)
        throws InvalidInputException, ConflictException {
        this.roleService.addRole(roleDto);
    }

    @Override
    public void updateUser(UserDto user)
        throws InvalidInputException, NotFoundException {
        this.userService.updateUser(user);
    }

    @Override
    public RoleDto getRole(RoleDto queryObject) throws NotFoundException {
        return this.roleService.getRole(queryObject);
    }

    // This method belongs to the UserService, but because it uses the client and not the Table,
    // it requires quite a big refactoring to put it there. It will be easier to do it after the
    // transition to sdk2 where we will be using the client and DynamoBeans.
    @Override
    public UserScanResult fetchOnePageOfUsers(ScanDatabaseRequestV2 scanRequest) {
        var result = scanDynamoDb(scanRequest);
        var startMarkerForNextScan = result.lastEvaluatedKey();
        var retrievedUsers = parseUsersFromScanResult(result);
        var thereAreMoreEntries = thereAreMoreEntries(result);
        return new UserScanResult(retrievedUsers, startMarkerForNextScan, thereAreMoreEntries);
    }

    private boolean thereAreMoreEntries(ScanResponse result) {
        return nonNull(result.lastEvaluatedKey());
    }

    private ScanResponse scanDynamoDb(ScanDatabaseRequestV2 scanRequest) {
        var dynamoScanRequest = createScanDynamoRequest(scanRequest);
        return dynamoDbClient.scan(dynamoScanRequest);
    }

    private boolean databaseEntryIsUser(Map<String, AttributeValue> databaseEntry) {
        return Optional.ofNullable(databaseEntry)
            .map(item -> item.get(WithType.TYPE_FIELD))
            .map(fields -> UserDao.TYPE_VALUE.equals(fields.s()))
            .orElse(false);
    }

    private List<UserDto> parseUsersFromScanResult(ScanResponse result) {
        return result.items()
            .stream()
            .filter(this::databaseEntryIsUser)
            .map(UserDao.TABLE_SCHEMA::mapToItem)
            .map(UserDao::toUserDto)
            .collect(Collectors.toList());
    }

    private ScanRequest createScanDynamoRequest(ScanDatabaseRequestV2 input) {
        return ScanRequest.builder()
            .tableName(USERS_AND_ROLES_TABLE)
            .limit(input.getPageSize())
            .exclusiveStartKey(input.getStartMarker())
            .build();
    }
}
