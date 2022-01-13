package no.unit.nva.database;

import static no.unit.nva.database.Constants.AWS_REGION;
import java.net.URI;
import java.util.List;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DatabaseServiceImpl implements DatabaseService {

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

    @JacocoGenerated
    private static DynamoDbClient defaultDynamoClient() {
        return DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(AWS_REGION))
            .build();
    }
}
