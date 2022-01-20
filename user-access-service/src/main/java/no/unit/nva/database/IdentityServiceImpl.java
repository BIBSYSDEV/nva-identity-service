package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static no.unit.nva.database.Constants.AWS_REGION;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.internals.UserScanResult;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityServiceImpl implements IdentityService {

    public static final String DYNAMO_DB_CLIENT_NOT_SET_ERROR = "DynamoDb client has not been set";
    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceImpl.class);

    private final UserService userService;
    private final RoleService roleService;
    private final AmazonDynamoDB dynamoDbClient;

    @JacocoGenerated
    public IdentityServiceImpl() {
        this(defaultDynamoClient());
    }

    public IdentityServiceImpl(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        var table = createTable(dynamoDbClient);
        this.roleService = new RoleService(table);
        this.userService = new UserService(table, roleService);
    }

    @Override
    public UserDto getUser(UserDto queryObject)
        throws NotFoundException, InvalidInputException {
        return userService.getUser(queryObject);
    }

    @Override
    public List<UserDto> listUsers(String institutionId) {
        return userService.listUsers(institutionId);
    }

    @Override
    public void addUser(UserDto user) throws ConflictException, InvalidInputException {
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
    public UserScanResult fetchOnePageOfUsers(ScanDatabaseRequest scanRequest) {
        var result = scanDynamoDb(scanRequest);
        var startMarkerForNextScan = result.getLastEvaluatedKey();
        var retrievedUsers = parseUsersFromScanResult(result);
        var thereAreMoreEntries = thereAreMoreEntries(result);
        return new UserScanResult(retrievedUsers, startMarkerForNextScan, thereAreMoreEntries);
    }

    protected static Table createTable(AmazonDynamoDB dynamoDbClient) {
        assertDynamoClientIsNotNull(dynamoDbClient);
        return new Table(dynamoDbClient, USERS_AND_ROLES_TABLE);
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoClient() {
        return AmazonDynamoDBClientBuilder.standard()
            .withRegion(AWS_REGION)
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();
    }

    private static void assertDynamoClientIsNotNull(AmazonDynamoDB dynamoDbClient) {
        attempt(() -> requireNonNull(dynamoDbClient))
            .orElseThrow(IdentityServiceImpl::logErrorWithDynamoClientAndThrowException);
    }

    private static RuntimeException logErrorWithDynamoClientAndThrowException(Failure<AmazonDynamoDB> failure) {
        logger.error(DYNAMO_DB_CLIENT_NOT_SET_ERROR);
        return new RuntimeException(failure.getException());
    }

    private boolean thereAreMoreEntries(ScanResult result) {
        return nonNull(result.getLastEvaluatedKey());
    }

    private ScanResult scanDynamoDb(ScanDatabaseRequest scanRequest) {
        var dynamoScanRequest = createScanDynamoRequest(scanRequest);
        return dynamoDbClient.scan(dynamoScanRequest);
    }

    private boolean databaseEntryIsUser(Item item) {
        return item.getString(WithType.TYPE_FIELD).equals(UserDb.TYPE);
    }

    private List<UserDto> parseUsersFromScanResult(ScanResult result) {
        return result.getItems().stream()
            .map(ItemUtils::toItem)
            .filter(this::databaseEntryIsUser)
            .map(Item::toJSON)
            .map(UserDb::fromJson)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());
    }

    private ScanRequest createScanDynamoRequest(ScanDatabaseRequest input) {
        return new ScanRequest()
            .withTableName(USERS_AND_ROLES_TABLE)
            .withLimit(input.getPageSize())
            .withExclusiveStartKey(input.getStartMarker());
    }
}
