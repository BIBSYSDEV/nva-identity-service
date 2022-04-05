package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SEARCH_USERS_BY_CRISTIN_IDENTIFIERS;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.dao.UserDao;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class UserService extends DatabaseSubService {

    public static final String USER_NOT_FOUND_MESSAGE = "Could not find user with username: ";
    public static final String GET_USER_DEBUG_MESSAGE = "Getting user: ";
    public static final String ADD_USER_DEBUG_MESSAGE = "Adding user: ";
    public static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "User already exists: ";
    public static final URI EMPTY_CRISTIN_ORG_ID = null;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final RoleService roleService;
    private final DynamoDbTable<UserDao> table;
    private final DynamoDbIndex<UserDao> cristinCredentialsIndex;
    private final DynamoDbIndex<UserDao> institutionsIndex;

    public UserService(DynamoDbClient client, RoleService roleService) {
        super(client);
        this.roleService = roleService;
        this.table = this.client.table(IdentityService.USERS_AND_ROLES_TABLE, UserDao.TABLE_SCHEMA);
        this.institutionsIndex = this.table.index(SEARCH_USERS_BY_INSTITUTION_INDEX_NAME);
        this.cristinCredentialsIndex = this.table.index(SEARCH_USERS_BY_CRISTIN_IDENTIFIERS);
    }

    /**
     * Fetches a user from the database that has the username specified in the input.
     *
     * @param queryObject the DTO containing the search information.
     * @return the DTO of the user in the database.
     * @throws NotFoundException when there is no use with that username
     */
    public UserDto getUser(UserDto queryObject) {
        return getUserAsOptional(queryObject)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + queryObject.getUsername()));
    }

    /**
     * List of users for a specified institution.
     *
     * @param institutionId the id of the institution
     * @return all users of the specified institution.
     */

    public List<UserDto> listUsers(URI institutionId) {
        QueryEnhancedRequest listUsersQuery = createListUsersByInstitutionQuery(institutionId);
        var result = institutionsIndex.query(listUsersQuery);

        var users = result.stream()
            .map(Page::items)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        return users.stream().map(UserDao::toUserDto)
            .collect(Collectors.toList());
    }

    /**
     * Adds a user.
     *
     * @param user the user to be added.
     * @return the user to be created
     * @throws ConflictException when the entry exists.
     */
    public UserDto addUser(UserDto user) {
        logger.debug(ADD_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(user));
        checkUserDoesNotAlreadyExist(user);
        UserDao databaseEntryWithSyncedRoles = syncRoleDetails(UserDao.fromUserDto(user));
        table.putItem(databaseEntryWithSyncedRoles);
        return databaseEntryWithSyncedRoles.toUserDto();
    }

    /**
     * Update an existing user.
     *
     * @param updateObject the updated user information.
     * @throws InvalidInputException when the input entry is invalid.
     * @throws NotFoundException     when there is no user with the same username in the database.
     */
    public void updateUser(UserDto updateObject) {

        UserDto existingUser = getExistingUserOrSendNotFoundError(updateObject);
        UserDao updatedObjectWithSyncedRoles = syncRoleDetails(UserDao.fromUserDto(updateObject));
        if (userHasChanged(existingUser, updatedObjectWithSyncedRoles)) {
            updateTable(updatedObjectWithSyncedRoles);
        }
    }

    public List<UserDto> getUsersByByCristinId(URI cristinPersonId) {
        var request = createQueryForSearchingInCristinCredentialsIndex(cristinPersonId, EMPTY_CRISTIN_ORG_ID);
        var result = cristinCredentialsIndex.query(request);
        return result.stream()
            .map(Page::items)
            .flatMap(Collection::stream)
            .map(UserDao::toUserDto).collect(Collectors.toList());
    }

    public UserDto getUsersByByCristinIdAndCristinOrgId(URI cristinPersonId, URI cristinOrgId) {
        var request = createQueryForSearchingInCristinCredentialsIndex(cristinPersonId, cristinOrgId);
        var result = cristinCredentialsIndex.query(request);
        return result.stream()
            .map(Page::items)
            .flatMap(Collection::stream)
            .map(UserDao::toUserDto).collect(SingletonCollector.collect());
    }

    private QueryEnhancedRequest createQueryForSearchingInCristinCredentialsIndex(URI cristinPersonId,
                                                                                  URI cristinOrgId) {
        var key = Optional.ofNullable(cristinOrgId)
            .map(orgId -> Key.builder().partitionValue(cristinPersonId.toString()).sortValue(orgId.toString()))
            .orElseGet(() -> Key.builder().partitionValue(cristinPersonId.toString()))
            .build();
        return QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(key))
            .build();
    }

    private UserDao syncRoleDetails(UserDao updateObject) {
        return userWithSyncedRoles(updateObject);
    }

    private Optional<UserDto> getUserAsOptional(UserDto queryObject) {
        logger.debug(GET_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        UserDto searchResult = attemptToFetchObject(queryObject);
        return Optional.ofNullable(searchResult);
    }

    private QueryEnhancedRequest createListUsersByInstitutionQuery(URI institution) {
        return QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(institution.toString()).build()))
            .consistentRead(false)
            .build();
    }

    private void checkUserDoesNotAlreadyExist(UserDto user) {
        if (userAlreadyExists(user)) {
            throw new ConflictException(USER_ALREADY_EXISTS_ERROR_MESSAGE + user.getUsername());
        }
    }

    private UserDto getExistingUserOrSendNotFoundError(UserDto queryObject) {
        return getUserAsOptional(queryObject)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + queryObject.getUsername()));
    }

    private boolean userAlreadyExists(UserDto user) {
        return this.getUserAsOptional(user).isPresent();
    }

    private UserDto attemptToFetchObject(UserDto queryObject) {
        UserDao userDao = attempt(() -> UserDao.fromUserDto(queryObject))
            .map(this::fetchItem)
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(userDao) ? userDao.toUserDto() : null;
    }

    private UserDao fetchItem(UserDao userDao) {
        return table.getItem(userDao);
    }

    private boolean userHasChanged(UserDto existingUser, UserDao desiredUpdateWithSyncedRoles) {
        return !desiredUpdateWithSyncedRoles.equals(UserDao.fromUserDto(existingUser));
    }

    private void updateTable(UserDao userUpdateWithSyncedRoles) {
        table.putItem(userUpdateWithSyncedRoles);
    }

    private UserDao userWithSyncedRoles(UserDao currentUser) {
        List<RoleDb> roles = currentRoles(currentUser);
        return currentUser.copy().withRoles(roles).build();
    }

    // TODO: use batch query for minimizing the cost.
    private List<RoleDb> currentRoles(UserDao currentUser) {
        return currentUser.getRolesNonNull()
            .stream()
            .map(roleService::fetchRoleDb)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
