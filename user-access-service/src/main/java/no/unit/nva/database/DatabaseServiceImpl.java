package no.unit.nva.database;

import static java.util.Objects.requireNonNull;
import static no.unit.nva.database.Constants.AWS_REGION;
import static nva.commons.core.attempt.Try.attempt;

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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DatabaseServiceImpl implements DatabaseService {

    public static final String DYNAMO_DB_CLIENT_NOT_SET_ERROR = "DynamoDb client has not been set";

    private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    private final UserService userService;
    private final RoleService roleService;

    @JacocoGenerated
    public DatabaseServiceImpl() {
        this(defaultDynamoClient());
    }

    public DatabaseServiceImpl(DynamoDbClient dynamoDbClient) {
        super();
        this.roleService = new RoleService(dynamoDbClient);
        this.userService = new UserService(dynamoDbClient, roleService);
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


    @JacocoGenerated
    private static DynamoDbClient defaultDynamoClient() {
       return DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(AWS_REGION))
            .build();
    }

}
