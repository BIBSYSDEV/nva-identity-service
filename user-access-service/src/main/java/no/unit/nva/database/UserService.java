package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class UserService extends DatabaseSubService {

    public static final String USER_NOT_FOUND_MESSAGE = "Could not find user with username: ";
    public static final String GET_USER_DEBUG_MESSAGE = "Getting user: ";
    public static final String ADD_USER_DEBUG_MESSAGE = "Adding user: ";
    public static final String UPDATE_USER_DEBUG_MESSAGE = "Updating user: ";
    public static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "User already exists: ";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final DynamoDbIndex<UserDb> institutionsIndex;
    private final RoleService roleService;
    private final DynamoDbTable<UserDb> table;

    public UserService(DynamoDbClient client, RoleService roleService) {
        super(client);
        this.roleService = roleService;
        this.table = this.client.table(DatabaseService.USERS_AND_ROLES_TABLE_NAME, TableSchema.fromClass(UserDb.class));
        this.institutionsIndex = this.table.index(SEARCH_USERS_BY_INSTITUTION_INDEX_NAME);
    }

    /**
     * Fetches a user from the database that has the username specified in the input.
     *
     * @param queryObject the DTO containing the search information.
     * @return the DTO of the user in the database.
     * @throws NotFoundException when there is no use with that username
     */
    public UserDto getUser(UserDto queryObject)
        throws NotFoundException, InvalidInputException {
        return getUserAsOptional(queryObject)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + queryObject.getUsername()));
    }

    /**
     * List of users for a specified institution.
     *
     * @param institutionIdentifier the identifier of the institution
     * @return all users of the specified institution.
     */
    public List<UserDto> listUsers(String institutionIdentifier) {
        QueryEnhancedRequest listUsersQuery = createListUsersByInstitutionQuery(institutionIdentifier);
        var result=institutionsIndex.query(listUsersQuery);

        return result.stream()
            .map(Page::items)
            .flatMap(Collection::stream)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());

    }

    /**
     * Adds a user.
     *
     * @param user the user to be added.
     * @throws ConflictException when the entry exists.
     */
    public void addUser(UserDto user) throws ConflictException {
        logger.debug(ADD_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(user));
        checkUserDoesNotAlreadyExist(user);
        UserDb databaseEntryWithSyncedRoles = syncRoleDetails(UserDb.fromUserDto(user));
        table.putItem(databaseEntryWithSyncedRoles);
    }

    /**
     * Update an existing user.
     *
     * @param updateObject the updated user information.
     * @throws InvalidInputException when the input entry is invalid.
     * @throws NotFoundException     when there is no user with the same username in the database.
     */
    public void updateUser(UserDto updateObject)
        throws InvalidInputException, NotFoundException {

        logger.debug(UPDATE_USER_DEBUG_MESSAGE + updateObject.toJsonString());
        UserDto existingUser = getExistingUserOrSendNotFoundError(updateObject);
        UserDb updatedObjectWithSyncedRoles = syncRoleDetails(UserDb.fromUserDto(updateObject));
        if (userHasChanged(existingUser, updatedObjectWithSyncedRoles)) {
            updateTable(updatedObjectWithSyncedRoles);
        }
    }

    private UserDb syncRoleDetails(UserDb updateObject) {
        return userWithSyncedRoles(updateObject);
    }

    private Optional<UserDto> getUserAsOptional(UserDto queryObject) {
        logger.debug(GET_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        UserDto searchResult = attemptToFetchObject(queryObject);
        return Optional.ofNullable(searchResult);
    }

    private QueryEnhancedRequest createListUsersByInstitutionQuery(String institution) {
        return QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(institution).build()))
            .consistentRead(false)
            .build();
    }

    private void checkUserDoesNotAlreadyExist(UserDto user) throws ConflictException {
        if (userAlreadyExists(user)) {
            throw new ConflictException(USER_ALREADY_EXISTS_ERROR_MESSAGE + user.getUsername());
        }
    }

    private UserDto getExistingUserOrSendNotFoundError(UserDto queryObject)
        throws NotFoundException {
        return getUserAsOptional(queryObject)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + queryObject.getUsername()));
    }

    private boolean userAlreadyExists(UserDto user) {
        return this.getUserAsOptional(user).isPresent();
    }

    private UserDto attemptToFetchObject(UserDto queryObject) {
        UserDb userDb = attempt(() -> UserDb.fromUserDto(queryObject))
            .map(this::fetchItem)
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(userDb) ? userDb.toUserDto() : null;
    }

    private UserDb fetchItem(UserDb userDb) {
        return table.getItem(userDb);
    }

    private boolean userHasChanged(UserDto existingUser, UserDb desiredUpdateWithSyncedRoles) {
        return !desiredUpdateWithSyncedRoles.equals(UserDb.fromUserDto(existingUser));
    }

    private void updateTable(UserDb userUpdateWithSyncedRoles) {
        table.putItem(userUpdateWithSyncedRoles);
    }

    private UserDb userWithSyncedRoles(UserDb currentUser) {
        List<RoleDb> roles = currentRoles(currentUser);
        return currentUser.copy().withRoles(roles).build();
    }

    // TODO: use batch query for minimizing the cost.
    private List<RoleDb> currentRoles(UserDb currentUser) {
        return currentUser
            .getRoles()
            .stream()
            .map(roleService::fetchRoleDao)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
