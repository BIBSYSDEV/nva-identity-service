package no.unit.nva.database;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.database.Constants.AWS_REGION;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.List;

import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseServiceImpl implements DatabaseService {

    public static final String DYNAMO_DB_CLIENT_NOT_SET_ERROR = "DynamoDb client has not been set";

    private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private final UserService userService;
    private final RoleService roleService;

    @JacocoGenerated
    public DatabaseServiceImpl() {
        this(defaultDynamoClient(), new Environment());
    }

    public DatabaseServiceImpl(AmazonDynamoDB dynamoDbClient, Environment environment) {
        this(createTable(dynamoDbClient, environment));
    }

    public DatabaseServiceImpl(Table table) {
        super();
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
    public RoleDto getRole(RoleDto queryObject) throws  NotFoundException {
        return this.roleService.getRole(queryObject);
    }

    protected static Table createTable(AmazonDynamoDB dynamoDbClient, Environment environment) {
        assertDynamoClientIsNotNull(dynamoDbClient);
        String tableName = environment.readEnv(USERS_AND_ROLES_TABLE_NAME_ENV_VARIABLE);
        return new Table(dynamoDbClient, tableName);
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
            .orElseThrow(DatabaseServiceImpl::logErrorWithDynamoClientAndThrowException);
    }

    private static RuntimeException logErrorWithDynamoClientAndThrowException(Failure<AmazonDynamoDB> failure) {
        logger.error(DYNAMO_DB_CLIENT_NOT_SET_ERROR);
        return new RuntimeException(failure.getException());
    }
}
