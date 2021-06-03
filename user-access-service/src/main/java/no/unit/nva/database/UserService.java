package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService extends DatabaseSubService {

    public static final String USER_NOT_FOUND_MESSAGE = "Could not find user with username: ";
    public static final String GET_USER_DEBUG_MESSAGE = "Getting user: ";
    public static final String ADD_USER_DEBUG_MESSAGE = "Adding user: ";
    public static final String UPDATE_USER_DEBUG_MESSAGE = "Updating user: ";
    public static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "User already exists: ";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final Index institutionsIndex;
    private final RoleService roleService;

    public UserService(Table table, RoleService roleService) {
        super(table);
        this.roleService = roleService;
        this.institutionsIndex = this.table.getIndex(SEARCH_USERS_BY_INSTITUTION_INDEX_NAME);
    }

    /**
     * Fetches a user from the database that has the username specified in the input.
     *
     * @param queryObject the DTO containing the search information.
     * @return the DTO of the user in the database.
     * @throws InvalidEntryInternalException when the entry stored in the database is invalid
     * @throws NotFoundException             when there is no use with that username
     */
    public UserDto getUser(UserDto queryObject) throws InvalidEntryInternalException, NotFoundException {
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
        QuerySpec listUsersQuery = createListUsersByInstitutionQuery(institutionIdentifier);
        List<Item> items = toList(institutionsIndex.query(listUsersQuery));

        return items.stream()
            .map(item -> UserDb.fromItem(item, UserDb.class))
            .map(attempt(UserDb::toUserDto))
            .flatMap(Try::stream)
            .collect(Collectors.toList());
    }

    /**
     * Adds a user.
     *
     * @param user the user to be added.
     * @throws InvalidEntryInternalException when a user with same username exists and the entry in the database is
     *                                       invalid.
     * @throws ConflictException             when the entry exists.
     * @throws InvalidInputException         when the input entry is not valid.
     */
    public void addUser(UserDto user) throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        logger.debug(ADD_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(user));

        validate(user);
        checkUserDoesNotAlreadyExist(user);
        UserDb databaseEntryWithSyncedRoles = syncRoleDetails(UserDb.fromUserDto(user));
        table.putItem(databaseEntryWithSyncedRoles.toItem());
    }

    /**
     * Update an existing user.
     *
     * @param updateObject the updated user information.
     * @throws InvalidEntryInternalException when a user with same username exists and the entry in the database is
     *                                       invalid.
     * @throws InvalidInputException         when the input entry is invalid.
     * @throws NotFoundException             when there is no user with the same username in the database.
     */
    public void updateUser(UserDto updateObject)
        throws InvalidEntryInternalException, InvalidInputException, NotFoundException {

        logger.debug(UPDATE_USER_DEBUG_MESSAGE + updateObject.toJsonString());
        validate(updateObject);
        UserDto existingUser = getExistingUserOrSendNotFoundError(updateObject);
        UserDb updatedObjectWithSyncedRoles = syncRoleDetails(UserDb.fromUserDto(updateObject));
        if (userHasChanged(existingUser, updatedObjectWithSyncedRoles)) {
            updateTable(updatedObjectWithSyncedRoles);
        }
    }

    private UserDb syncRoleDetails(UserDb updateObject) throws InvalidEntryInternalException {
        return userWithSyncedRoles(updateObject);
    }

    private Optional<UserDto> getUserAsOptional(UserDto queryObject) throws InvalidEntryInternalException {
        logger.debug(GET_USER_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        UserDto searchResult = attemptToFetchObject(queryObject);
        return Optional.ofNullable(searchResult);
    }

    private QuerySpec createListUsersByInstitutionQuery(String institution) {
        return new QuerySpec().withHashKey(SECONDARY_INDEX_1_HASH_KEY, institution)
            .withConsistentRead(false);
    }

    private void checkUserDoesNotAlreadyExist(UserDto user) throws InvalidEntryInternalException, ConflictException {
        if (userAlreadyExists(user)) {
            throw new ConflictException(USER_ALREADY_EXISTS_ERROR_MESSAGE + user.getUsername());
        }
    }

    private UserDto getExistingUserOrSendNotFoundError(UserDto queryObject)
        throws NotFoundException, InvalidEntryInternalException {
        return getUserAsOptional(queryObject)
            .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE + queryObject.getUsername()));
    }

    private boolean userAlreadyExists(UserDto user) throws InvalidEntryInternalException {
        return this.getUserAsOptional(user).isPresent();
    }

    private List<Item> toList(ItemCollection<QueryOutcome> searchResult) {
        List<Item> items = new ArrayList<>();
        for (Item item : searchResult) {
            items.add(item);
        }
        return items;
    }

    private UserDto attemptToFetchObject(UserDto queryObject) throws InvalidEntryInternalException {
        UserDb userDb = attempt(() -> UserDb.fromUserDto(queryObject))
            .map(this::fetchItem)
            .map(item -> UserDb.fromItem(item, UserDb.class))
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(userDb) ? userDb.toUserDto() : null;
    }

    private boolean userHasChanged(UserDto existingUser, UserDb desiredUpdateWithSyncedRoles)
        throws InvalidEntryInternalException {
        return !desiredUpdateWithSyncedRoles.equals(UserDb.fromUserDto(existingUser));
    }

    private void updateTable(UserDb userUpdateWithSyncedRoles) {
        table.putItem(userUpdateWithSyncedRoles.toItem());
    }

    private UserDb userWithSyncedRoles(UserDb currentUser) throws InvalidEntryInternalException {
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
