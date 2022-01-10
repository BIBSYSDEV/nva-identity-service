package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.database.EntityUtils.SOME_ROLENAME;
import static no.unit.nva.database.EntityUtils.createRole;
import static no.unit.nva.database.RoleService.ROLE_ALREADY_EXISTS_ERROR_MESSAGE;
import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static no.unit.nva.database.UserService.USER_ALREADY_EXISTS_ERROR_MESSAGE;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.INCLUDE_NESTED_UNITS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

public class DatabaseServiceTest extends DatabaseAccessor {

    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();
    private static final String SOME_USERNAME = "someusername";
    private static final String SOME_OTHER_USERNAME = "someotherusername";
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";
    private static final String SOME_INSTITUTION = "SomeInstitution";
    private static final String SOME_OTHER_ROLE = "SOME_OTHER_ROLE";
    private static final String SOME_OTHER_INSTITUTION = "Some other institution";
    private DatabaseService db;
    private DynamoDbEnhancedClient enhancedClient;
    private

    @BeforeEach
    public void init() {
        db = createDatabaseServiceUsingLocalStorage();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(localDynamo).build();
    }

    @Test
    public void databaseServiceHasAMethodForInsertingAUser()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, BadRequestException {
        UserDto user = createSampleUserWithoutInstitutionOrRoles(SOME_USERNAME);
        db.addUser(user);
    }

    @DisplayName("getRole() returns non empty role when role-name exists in database")
    @Test
    public void databaseServiceReturnsNonEmptyRoleWhenRoleNameExistsInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException {
        RoleDto insertedRole = createSampleRoleAndAddToDb(SOME_ROLENAME);
        RoleDto savedRole = db.getRole(insertedRole);

        assertThat(insertedRole, doesNotHaveEmptyValues());
        assertThat(savedRole, is(equalTo(insertedRole)));
    }

    @DisplayName("getRole() throws NotFoundException when the role-name does not exist in the database")
    @Test
    public void databaseServiceThrowsNotFoundExceptionWhenRoleNameDoesNotExist() throws InvalidEntryInternalException {
        RoleDto queryObject = createRole(SOME_ROLENAME);
        Executable action = () -> db.getRole(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @DisplayName("addRole() inserts valid role")
    @Test
    public void addRoleInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException {
        RoleDto insertedUser = createSampleRoleAndAddToDb(SOME_ROLENAME);
        RoleDto savedUser = db.getRole(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addRole() throws Exception when trying to save role without name")
    @Test
    public void addRoleShouldNotSaveUserWithoutUsername() throws InvalidEntryInternalException {
        RoleDto illegalRole = createIllegalRole();
        Executable illegalAction = () -> db.addRole(illegalRole);
        InvalidInputException exception = assertThrows(InvalidInputException.class, illegalAction);
        assertThat(exception.getMessage(), containsString(RoleDto.MISSING_ROLE_NAME_ERROR));
    }

    @DisplayName("addRole() throws ConflictException when trying to save user with existing username")
    @Test
    public void addRoleThrowsConflictExceptionWhenTryingToSaveAlreadyExistingUser()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException {

        String conflictingRoleName = SOME_ROLENAME;
        createSampleRoleAndAddToDb(conflictingRoleName);

        RoleDto conflictingRole = createRole(conflictingRoleName);

        Executable action = () -> db.addRole(conflictingRole);
        ConflictException exception = assertThrows(ConflictException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    @DisplayName("getUser() returns non empty user when username exists in database")
    @Test
    public void databaseServiceReturnsNonEmptyUserWhenUsernameExistsInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto savedUser = db.getUser(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("getUser() throws NotFoundException when the username does exist in the database")
    @Test
    public void databaseServiceThrowsNotFoundExceptionWhenUsernameDoesNotExist()
        throws InvalidEntryInternalException {
        UserDto queryObject = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        Executable action = () -> db.getUser(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @DisplayName("addUser() inserts valid user with institution and roles in database")
    @Test
    public void addUserInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto savedUser = db.getUser(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addUser() saves user with roles and without institution")
    @Test
    public void addUserSavesAUserWithoutInstitution() throws InvalidEntryInternalException, ConflictException,
                                                             InvalidInputException, NotFoundException,
                                                             BadRequestException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, null, SOME_ROLENAME);
        UserDto actualUser = db.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser.getInstitution(), is(equalTo(null)));
    }

    @DisplayName("addUser() saves user with institution without roles")
    @Test
    public void addUserShouldSaveUserWithoutRoles()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, null);
        UserDto actualUser = db.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("addUser() throws ConflictException when trying to save user with existing username")
    @Test
    public void addUserThrowsConflictExceptionWhenTryingToSaveAlreadyExistingUser()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException {

        String conflictingUsername = SOME_USERNAME;
        createSampleUserAndAddUserToDb(conflictingUsername, SOME_INSTITUTION, SOME_ROLENAME);

        UserDto conflictingUser = createUserWithRole(conflictingUsername,
                                                     SOME_OTHER_INSTITUTION, createRole(SOME_OTHER_ROLE));

        Executable action = () -> db.addUser(conflictingUser);
        ConflictException exception = assertThrows(ConflictException.class, action);
        assertThat(exception.getMessage(), containsString(USER_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    @Test
    public void addUserAddsCurrentlySavedVersionOfRoleInNewUser()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, NotFoundException,
               BadRequestException {
        RoleDto existingRole = createSampleRoleAndAddToDb(SOME_ROLENAME);
        assertThat(existingRole, doesNotHaveEmptyValues());

        UserDto userWithoutRoleDetails = createUserWithRoleReference(existingRole);
        db.addUser(userWithoutRoleDetails);
        UserDto savedUser = db.getUser(userWithoutRoleDetails);
        RoleDto actualRole = savedUser.getRoles().stream().collect(SingletonCollector.collect());
        assertThat(actualRole, is(equalTo(existingRole)));
    }

    @Test
    public void addUserDoesNotAddNonExistingRolesInCreatedUser()
        throws Exception {
        UserDto userWithNonExistingRole = createUserWithRole(SOME_USERNAME, SOME_INSTITUTION,
                                                             createRole(SOME_ROLENAME));
        db.addUser(userWithNonExistingRole);

        UserDto actualUser = db.getUser(userWithNonExistingRole);
        UserDto expectedUser = userWithNonExistingRole.copy().withRoles(Collections.emptyList()).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("updateUser() updates existing user with input user when input user is valid")
    @Test
    public void updateUserUpdatesAssignsCorrectVersionOfRoleInUser()
        throws Exception {
        RoleDto existingRole = createRole(SOME_ROLENAME);
        db.addRole(existingRole);
        UserDto existingUser = createUserWithRole(SOME_USERNAME, SOME_INSTITUTION, existingRole);
        db.addUser(existingUser);

        UserDto userUpdate = userUpdateWithRoleMissingAccessRights(existingUser);

        UserDto expectedUser = existingUser.copy().withGivenName(SOME_GIVEN_NAME).build();
        db.updateUser(userUpdate);
        UserDto actualUser = db.getUser(expectedUser);
        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser, is(not(sameInstance(expectedUser))));
    }

    @DisplayName("updateUser() updates existing user with input user when input user is valid")
    @Test
    public void updateUserUpdatesExistingUserWithInputUserWhenInputUserIsValid()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException,
               BadRequestException {
        UserDto existingUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto expectedUser = cloneAndChangeRole(existingUser);

        db.updateUser(expectedUser);
        UserDto actualUser = db.getUser(expectedUser);
        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser, is(not(sameInstance(expectedUser))));
    }

    @DisplayName("updateUser() throws NotFoundException when the input username does not exist")
    @Test
    public void updateUserThrowsNotFoundExceptionWhenTheInputUsernameDoesNotExist()
        throws InvalidEntryInternalException, BadRequestException {
        UserDto userUpdate = createSampleUser(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        Executable action = () -> db.updateUser(userUpdate);
        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @DisplayName("updateUser() throws Exception when the input is invalid ")
    @Test
    public void updateUserThrowsExceptionWhenTheInputIsInvalid()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto invalidUser = new UserDto();
        Executable action = () -> db.updateUser(invalidUser);
        assertThrows(RuntimeException.class, action);
    }

    @Test
    public void listUsersByInstitutionReturnsAllUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException {
        UserDto someUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto someOtherUser = createSampleUserAndAddUserToDb(SOME_OTHER_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        List<UserDto> queryResult = db.listUsers(SOME_INSTITUTION);
        assertThat(queryResult, containsInAnyOrder(someUser, someOtherUser));
    }

    @Test
    public void listUsersByInstitutionReturnsEmptyListWhenThereAreNoUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        List<UserDto> queryResult = db.listUsers(SOME_OTHER_INSTITUTION);
        assertThat(queryResult, is(empty()));
    }

    @Test
    public void roleDbWithAccessRightsIsSavedInDatabase() throws InvalidEntryInternalException {
        var accessRights = Set.of(AccessRight.APPROVE_DOI_REQUEST, AccessRight.REJECT_DOI_REQUEST);

        RoleDb roleWithAccessRights = RoleDb.newBuilder()
            .withAccessRights(accessRights)
            .withName(SOME_ROLENAME)
            .build();

        enhancedClient.table().putItem(roleWithAccessRights.toItem());

        Item savedRoleItem = fetchRoleDirectlyFromTable(table, roleWithAccessRights);

        RoleDb savedRole = RoleDb.fromItem(savedRoleItem, RoleDb.class);

        assertThat(savedRole, is(equalTo(roleWithAccessRights)));
    }

    @Test
    void userDbShouldBeWriteableToDatabase() throws InvalidEntryInternalException {
        UserDb sampleUser = UserDb.newBuilder().withUsername(SOME_USERNAME).build();

        Table table = DatabaseServiceImpl.createTable(initializeTestDatabase(), envWithTableName);
        Assertions.assertDoesNotThrow(() -> table.putItem(sampleUser.toItem()));
    }

    @Test
    void userDbShouldBeReadFromDatabaseWithoutDataLoss() throws InvalidEntryInternalException, BadRequestException {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomUri()), Set.of(randomUri()), INCLUDE_NESTED_UNITS);
        UserDb insertedUser = UserDb.newBuilder()
            .withUsername(SOME_USERNAME)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withInstitution(SOME_INSTITUTION)
            .withRoles(SAMPLE_ROLES)
            .withViewingScope(viewingScope)
            .build();
        Table table = clientToLocalDatabase();
        table.putItem(insertedUser.toItem());
        assertThat(insertedUser, doesNotHaveEmptyValues());
        Item item = table.getItem(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY, insertedUser.getPrimaryHashKey(),
                                  PRIMARY_KEY_RANGE_KEY, insertedUser.getPrimaryRangeKey());
        UserDb savedUser = UserDb.fromItem(item, UserDb.class);
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @Test
    void createTableThrowsExceptionWhenDynamoClientIsNull() {
        Executable action =
            () -> createTable(null, mockEnvironment());
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), instanceOf(NullPointerException.class));
    }


    private static List<RoleDb> createSampleRoles() {
        try {
            return Collections.singletonList(randomRole());
        } catch (InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        }
    }

    private static RoleDb randomRole() {
        return RoleDb.newBuilder()
            .withName(randomString())
            .withAccessRights(Set.of(randomElement(AccessRight.values())))
            .build();
    }

    private UserDto createUserWithRoleReference(RoleDto existingRole)
        throws InvalidEntryInternalException, BadRequestException {
        RoleDto roleWithoutDetails = RoleDto.newBuilder().withName(existingRole.getRoleName()).build();
        RoleDto.newBuilder().withName(existingRole.getRoleName()).build();
        return createSampleUser(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME)
            .copy()
            .withRoles(Collections.singletonList(roleWithoutDetails))
            .build();
    }

    private Item fetchRoleDirectlyFromTable(Table table, RoleDb roleWithAccessRights) {
        return table.getItem(PRIMARY_KEY_HASH_KEY, roleWithAccessRights.getPrimaryHashKey(),
                             PRIMARY_KEY_RANGE_KEY, roleWithAccessRights.getPrimaryRangeKey());
    }

    private UserDto userUpdateWithRoleMissingAccessRights(UserDto existingUser)
        throws InvalidEntryInternalException {
        RoleDto roleWithOnlyRolename = RoleDto.newBuilder().withName(SOME_ROLENAME).build();
        UserDto userUpdate = existingUser.copy()
            .withGivenName(SOME_GIVEN_NAME)
            .withRoles(Collections.singletonList(roleWithOnlyRolename))
            .build();
        return userUpdate;
    }

    private UserDto createUserWithRole(String someUsername, String someInstitution, RoleDto existingRole)
            throws InvalidEntryInternalException, BadRequestException {
        ViewingScope viewingScope = new ViewingScope(Set.of(randomUri()), Set.of(randomUri()), false);
        return UserDto.newBuilder().withUsername(someUsername)
            .withInstitution(someInstitution)
            .withRoles(Collections.singletonList(existingRole))
            .withViewingScope(viewingScope)
            .build();
    }

    private UserDto createSampleUserWithoutInstitutionOrRoles(String username)
        throws InvalidEntryInternalException, BadRequestException {
        return createSampleUser(username, null, null);
    }

    private RoleDto createIllegalRole() throws InvalidEntryInternalException {
        RoleDto illegalRole = createRole(SOME_ROLENAME);
        illegalRole.setRoleName(null);
        return illegalRole;
    }

    private UserDto cloneAndChangeRole(UserDto existingUser) throws InvalidEntryInternalException {
        RoleDto someOtherRole = createRole(SOME_OTHER_ROLE);
        addRoleToDb(someOtherRole);
        return existingUser.copy().withRoles(Collections.singletonList(someOtherRole)).build();
    }

    private UserDto createSampleUserAndAddUserToDb(String username, String institution, String roleName)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, BadRequestException {
        UserDto userDto = createSampleUser(username, institution, roleName);
        Set<RoleDto> roles = userDto.getRoles();
        roles.stream().forEach(this::addRoleToDb);
        db.addUser(userDto);
        return userDto;
    }

    private void addRoleToDb(RoleDto role) {
        try {
            db.addRole(role);
        } catch (InvalidInputException | InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        } catch (ConflictException e) {
            System.out.println("Role exists:" + role.toString());
        }
    }

    private UserDto createSampleUser(String username, String institution, String roleName)
        throws InvalidEntryInternalException, BadRequestException {
        return UserDto.newBuilder()
            .withRoles(createRoleList(roleName))
            .withInstitution(institution)
            .withUsername(username)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withViewingScope(randomViewingScope())
            .build();
    }

    private ViewingScope randomViewingScope() throws BadRequestException {
        return new ViewingScope(Set.of(randomUri()), Set.of(randomUri()),INCLUDE_NESTED_UNITS);
    }

    private RoleDto createSampleRoleAndAddToDb(String roleName)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        RoleDto roleDto = createRole(roleName);
        db.addRole(roleDto);
        return roleDto;
    }

    private List<RoleDto> createRoleList(String rolename) throws InvalidEntryInternalException {
        if (nonNull(rolename)) {
            RoleDto roleDto = createRole(rolename);
            return Collections.singletonList(roleDto);
        } else {
            return Collections.emptyList();
        }
    }


}
