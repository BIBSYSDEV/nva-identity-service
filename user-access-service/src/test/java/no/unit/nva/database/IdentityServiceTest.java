package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.database.EntityUtils.SOME_ROLENAME;
import static no.unit.nva.database.EntityUtils.createRole;
import static no.unit.nva.database.IdentityService.USERS_AND_ROLES_TABLE;
import static no.unit.nva.database.RoleService.ROLE_ALREADY_EXISTS_ERROR_MESSAGE;
import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static no.unit.nva.database.UserService.USER_ALREADY_EXISTS_ERROR_MESSAGE;
import static no.unit.nva.database.UserService.USER_NOT_FOUND_MESSAGE;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.dao.UserDao;
import no.unit.nva.useraccessservice.dao.ViewingScopeDb;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

public class IdentityServiceTest extends DatabaseAccessor {

    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();
    private static final String SOME_USERNAME = "someusername";
    private static final String SOME_OTHER_USERNAME = randomString();
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    private static final URI SOME_INSTITUTION = randomCristinOrgId();
    private static final String SOME_OTHER_ROLE = "SOME_OTHER_ROLE";
    private static final URI SOME_OTHER_INSTITUTION = randomCristinOrgId();
    private IdentityService identityService;
    private DynamoDbTable<RoleDb> rolesTable;
    private DynamoDbTable<UserDao> usersTable;

    @BeforeEach
    public void init() {
        identityService = createDatabaseServiceUsingLocalStorage();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(localDynamo).build();
        rolesTable = enhancedClient.table(IdentityService.USERS_AND_ROLES_TABLE, RoleDb.TABLE_SCHEMA);
        usersTable = enhancedClient.table(IdentityService.USERS_AND_ROLES_TABLE, UserDao.TABLE_SCHEMA);
    }

    @Test
    void databaseServiceHasAMethodForInsertingAUser()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        UserDto user = createSampleUserWithoutInstitutionOrRoles(SOME_USERNAME);
        identityService.addUser(user);
    }

    @DisplayName("getRole() returns non empty role when role-name exists in database")
    @Test
    void databaseServiceReturnsNonEmptyRoleWhenRoleNameExistsInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException {
        RoleDto insertedRole = createSampleRoleAndAddToDb(SOME_ROLENAME);
        RoleDto savedRole = identityService.getRole(insertedRole);

        assertThat(insertedRole, doesNotHaveEmptyValues());
        assertThat(savedRole, is(equalTo(insertedRole)));
    }

    @DisplayName("getRole() throws NotFoundException when the role-name does not exist in the database")
    @Test
    public void databaseServiceThrowsNotFoundExceptionWhenRoleNameDoesNotExist() throws InvalidEntryInternalException {
        RoleDto queryObject = createRole(SOME_ROLENAME);
        Executable action = () -> identityService.getRole(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @DisplayName("addRole() inserts valid role")
    @Test
    public void addRoleInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException {
        RoleDto insertedUser = createSampleRoleAndAddToDb(SOME_ROLENAME);
        RoleDto savedUser = identityService.getRole(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addRole() throws Exception when trying to save role without name")
    @Test
    public void addRoleShouldNotSaveUserWithoutUsername() throws InvalidEntryInternalException {
        RoleDto illegalRole = createIllegalRole();
        Executable illegalAction = () -> identityService.addRole(illegalRole);
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

        Executable action = () -> identityService.addRole(conflictingRole);
        ConflictException exception = assertThrows(ConflictException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    @DisplayName("getUser() returns non empty user when username exists in database")
    @Test
    public void databaseServiceReturnsNonEmptyUserWhenUsernameExistsInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto savedUser = identityService.getUser(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("getUser() throws NotFoundException when the username does exist in the database")
    @Test
    public void databaseServiceThrowsNotFoundExceptionWhenUsernameDoesNotExist()
        throws InvalidEntryInternalException {
        UserDto queryObject = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        Executable action = () -> identityService.getUser(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @Test
    void addUserInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        assertThat(insertedUser, doesNotHaveEmptyValues());
        UserDto savedUser = identityService.getUser(insertedUser);

        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addUser() saves user with roles and without institution")
    @Test
    void addUserSavesAUserWithoutInstitution() throws InvalidEntryInternalException, ConflictException,
                                                      InvalidInputException, NotFoundException,
                                                      BadRequestException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, null, SOME_ROLENAME);
        UserDto actualUser = identityService.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser.getInstitution(), is(equalTo(null)));
    }

    @DisplayName("addUser() saves user with institution without roles")
    @Test
    public void addUserShouldSaveUserWithoutRoles()
        throws InvalidEntryInternalException, ConflictException, InvalidInputException, NotFoundException,
               BadRequestException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, null);
        UserDto actualUser = identityService.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("addUser() throws ConflictException when trying to save user with existing username")
    @Test
    public void addUserThrowsConflictExceptionWhenTryingToSaveAlreadyExistingUser()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException,
               NotFoundException {

        String conflictingUsername = SOME_USERNAME;
        createSampleUserAndAddUserToDb(conflictingUsername, SOME_INSTITUTION, SOME_ROLENAME);

        UserDto conflictingUser = createUserWithRole(conflictingUsername,
                                                     SOME_OTHER_INSTITUTION, createRole(SOME_OTHER_ROLE));

        Executable action = () -> identityService.addUser(conflictingUser);
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
        identityService.addUser(userWithoutRoleDetails);
        UserDto savedUser = identityService.getUser(userWithoutRoleDetails);
        RoleDto actualRole = savedUser.getRoles().stream().collect(SingletonCollector.collect());
        assertThat(actualRole, is(equalTo(existingRole)));
    }

    @Test
    void addUserDoesNotAddNonExistingRolesInCreatedUser() {
        UserDto userWithNonExistingRole = createUserWithRole(SOME_USERNAME, SOME_INSTITUTION,
                                                             createRole(SOME_ROLENAME));
        identityService.addUser(userWithNonExistingRole);

        UserDto actualUser = identityService.getUser(userWithNonExistingRole);
        UserDto expectedUser = userWithNonExistingRole.copy().withRoles(Collections.emptyList()).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("updateUser() updates existing user with input user when input user is valid")
    @Test
    void updateUserUpdatesAssignsCorrectVersionOfRoleInUser() {
        RoleDto existingRole = createRole(SOME_ROLENAME);
        identityService.addRole(existingRole);
        UserDto existingUser = createUserWithRole(SOME_USERNAME, SOME_INSTITUTION, existingRole);
        identityService.addUser(existingUser);

        UserDto userUpdate = userUpdateWithRoleMissingAccessRights(existingUser);

        UserDto expectedUser = existingUser.copy().withGivenName(SOME_GIVEN_NAME).build();
        identityService.updateUser(userUpdate);
        UserDto actualUser = identityService.getUser(expectedUser);
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

        identityService.updateUser(expectedUser);
        UserDto actualUser = identityService.getUser(expectedUser);
        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser, is(not(sameInstance(expectedUser))));
    }

    @DisplayName("updateUser() throws NotFoundException when the input username does not exist")
    @Test
    public void updateUserThrowsNotFoundExceptionWhenTheInputUsernameDoesNotExist()
        throws InvalidEntryInternalException, BadRequestException {
        UserDto userUpdate = createSampleUser(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        Executable action = () -> identityService.updateUser(userUpdate);
        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @DisplayName("updateUser() throws Exception when the input is invalid ")
    @Test
    public void updateUserThrowsExceptionWhenTheInputIsInvalid()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException,
               NotFoundException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto invalidUser = new UserDto();
        Executable action = () -> identityService.updateUser(invalidUser);
        assertThrows(RuntimeException.class, action);
    }

    @Test
    public void listUsersByInstitutionReturnsAllUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException,
               NotFoundException {
        UserDto someUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        UserDto someOtherUser = createSampleUserAndAddUserToDb(SOME_OTHER_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        List<UserDto> queryResult = identityService.listUsers(SOME_INSTITUTION);
        assertThat(queryResult, containsInAnyOrder(someUser, someOtherUser));
    }

    @Test
    public void listUsersByInstitutionReturnsEmptyListWhenThereAreNoUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException, InvalidInputException, BadRequestException,
               NotFoundException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME);
        List<UserDto> queryResult = identityService.listUsers(SOME_OTHER_INSTITUTION);
        assertThat(queryResult, is(empty()));
    }

    @Test
    public void roleDbWithAccessRightsIsSavedInDatabase() throws InvalidEntryInternalException {
        var accessRights = Set.of(AccessRight.APPROVE_DOI_REQUEST, AccessRight.REJECT_DOI_REQUEST);

        RoleDb roleWithAccessRights = RoleDb.newBuilder()
            .withAccessRights(accessRights)
            .withName(SOME_ROLENAME)
            .build();

        rolesTable.putItem(roleWithAccessRights);

        var savedRole = fetchRoleDirectlyFromTable(roleWithAccessRights);
        assertThat(savedRole, is(equalTo(roleWithAccessRights)));
    }

    @Test
    void userDbShouldBeWriteableToDatabase() throws InvalidEntryInternalException {

        UserDao sampleUser = UserDao.newBuilder()
            .withUsername(SOME_USERNAME)
            .withInstitution(randomCristinOrgId())
            .build();
        usersTable.putItem(sampleUser);
        assertDoesNotThrow(() -> usersTable.putItem(sampleUser));
    }

    @Test
    void userDbShouldBeReadFromDatabaseWithoutDataLoss() throws InvalidEntryInternalException {

        UserDao insertedUser = UserDao.newBuilder()
            .withUsername(SOME_USERNAME)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withInstitution(SOME_INSTITUTION)
            .withRoles(SAMPLE_ROLES)
            .withViewingScope(ViewingScopeDb.fromViewingScope(randomViewingScope()))
            .withCristinId(randomUri())
            .withFeideIdentifier(randomString())
            .withInstitutionCristinId(randomCristinOrgId())
            .build();

        usersTable.putItem(insertedUser);
        assertThat(insertedUser, doesNotHaveEmptyValues());
        var savedUser = usersTable.getItem(Key.builder().partitionValue(insertedUser.getPrimaryKeyHashKey())
                                               .sortValue(insertedUser.getPrimaryKeyRangeKey())
                                               .build());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @Test
    void getRoleLogsWarningWhenNotFoundExceptionIsThrown() throws InvalidEntryInternalException {
        TestAppender testAppender = LogUtils.getTestingAppender(RoleService.class);
        RoleDto nonExistingRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        attempt(() -> databaseService.getRole(nonExistingRole));
        assertThat(testAppender.getMessages(),
                   StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @Test
    void getUserReturnsUserWhenUserHasEmptyValuesForCollections()
        throws InvalidInputException, ConflictException, NotFoundException {
        var user = UserDto.newBuilder().withUsername(randomString()).build();
        databaseService.addUser(user);
        var retrievedUser = databaseService.getUser(user);
        assertThat(retrievedUser, is(equalTo(user)));
    }

    @Test
    void shouldReturnPageOfResultsWhenScanDatabaseRequestIsSubmitted()
        throws InvalidInputException, ConflictException, BadRequestException, NotFoundException {
        int totalNumberOfUsers = 100;
        int pageSize = 15;
        createSampleUsers(totalNumberOfUsers);
        var request = new ScanDatabaseRequestV2(randomString(), pageSize, null);
        var firstPageOfUsers = databaseService.fetchOnePageOfUsers(request);
        var expectedFirstPageOfUsers = scanDatabaseDirectlyAndGetAllUsersInExpectedOrderIgnoringRoleEntries(pageSize);
        assertEquals(expectedFirstPageOfUsers, firstPageOfUsers.getRetrievedUsers());
        assertThat(firstPageOfUsers.getRetrievedUsers(), is(equalTo(expectedFirstPageOfUsers)));
    }

    @Test
    void shouldFetchAllUsersOfPersonBasedOnCristinPersonIdentifier() {
        var cristinPersonId = randomUri();
        var user1 = createUserAndAddUserToDb(cristinPersonId, randomCristinOrgId(), randomString());
        var user2 = createUserAndAddUserToDb(cristinPersonId, randomCristinOrgId(), randomString());
        var retrievedUsers = identityService.getUsersByCristinId(cristinPersonId);
        assertThat(retrievedUsers,containsInAnyOrder(user1, user2));
    }


    @Test
    void shouldFetchUsersOfPersonBasedOnCristinPersonIdAndCristinOrgId() {
        var cirstinPersonId = randomUri();
        var cristinOrgId = randomCristinOrgId();
        var expectedUser = createUserAndAddUserToDb(cirstinPersonId, cristinOrgId, randomString());
        var retrievedUser = identityService.getUserByCristinIdAndCristinOrgId(cirstinPersonId,cristinOrgId);
        assertThat(retrievedUser,is(equalTo(expectedUser)));
    }

    private UserDto createUserAndAddUserToDb(URI cristinId, URI cristinOrgId, String feideIdentifier) {
        var roles = createSampleRoles().stream()
            .map(RoleDb::toRoleDto)
            .peek(role -> identityService.addRole(role))
            .collect(Collectors.toList());
        var user = UserDto.newBuilder().withCristinId(cristinId)
            .withFeideIdentifier(feideIdentifier)
            .withCristinId(cristinId)
            .withFamilyName(randomString())
            .withGivenName(randomString())
            .withInstitution(randomUri())
            .withInstitutionCristinId(cristinOrgId)
            .withUsername(randomString())
            .withRoles(roles)
            .withViewingScope(randomViewingScope())
            .build();
        identityService.addUser(user);
        var savedUser = identityService.getUser(user);
        assertThat(user, doesNotHaveEmptyValues());
        assertThat(savedUser, doesNotHaveEmptyValues());
        return user;
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

    private void createSampleUsers(int numberOfUsers)
        throws InvalidInputException, ConflictException, BadRequestException, NotFoundException {
        for (int counter = 0; counter < numberOfUsers; counter++) {
            createSampleUserAndAddUserToDb(randomString(), randomUri(), randomString());
        }
    }

    private List<UserDto> scanDatabaseDirectlyAndGetAllUsersInExpectedOrderIgnoringRoleEntries(int pageSize) {
        return localDynamo.scan(ScanRequest.builder().tableName(USERS_AND_ROLES_TABLE).scanFilter(
                filterOutNonUserEntries()).limit(pageSize).build())
            .items()
            .stream()
            .map(UserDao.TABLE_SCHEMA::mapToItem)
            .map(UserDao::toUserDto)
            .collect(Collectors.toList());
    }

    private Map<String, Condition> filterOutNonUserEntries() {
        var primaryKeyStartsWithUserType = Condition.builder()
            .attributeValueList(userType())
            .comparisonOperator(ComparisonOperator.BEGINS_WITH)
            .build();
        return Map.of(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY, primaryKeyStartsWithUserType);
    }

    private AttributeValue userType() {
        return AttributeValue.builder().s(UserDao.TYPE_VALUE).build();
    }

    private UserDto createUserWithRoleReference(RoleDto existingRole)
        throws InvalidEntryInternalException, BadRequestException {
        RoleDto roleWithoutDetails = RoleDto.newBuilder().withRoleName(existingRole.getRoleName()).build();
        RoleDto.newBuilder().withRoleName(existingRole.getRoleName()).build();
        return createSampleUser(SOME_USERNAME, SOME_INSTITUTION, SOME_ROLENAME)
            .copy()
            .withRoles(Collections.singletonList(roleWithoutDetails))
            .build();
    }

    private RoleDb fetchRoleDirectlyFromTable(RoleDb roleWithAccessRights) {
        return rolesTable.getItem(Key.builder()
                                      .partitionValue(roleWithAccessRights.getPrimaryKeyHashKey())
                                      .sortValue(roleWithAccessRights.getPrimaryKeyRangeKey())
                                      .build());
    }

    private UserDto userUpdateWithRoleMissingAccessRights(UserDto existingUser)
        throws InvalidEntryInternalException {
        RoleDto roleWithOnlyRolename = RoleDto.newBuilder().withRoleName(SOME_ROLENAME).build();
        return existingUser.copy()
            .withGivenName(SOME_GIVEN_NAME)
            .withRoles(Collections.singletonList(roleWithOnlyRolename))
            .build();
    }

    private UserDto createUserWithRole(String someUsername, URI someInstitution, RoleDto existingRole)
        throws InvalidEntryInternalException {
        return UserDto.newBuilder().withUsername(someUsername)
            .withInstitution(someInstitution)
            .withRoles(Collections.singletonList(existingRole))
            .withViewingScope(randomViewingScope())
            .build();
    }

    private UserDto createSampleUserWithoutInstitutionOrRoles(String username)
        throws InvalidEntryInternalException {
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

    private UserDto createSampleUserAndAddUserToDb(String username, URI institution, String roleName)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException,
               NotFoundException {
        var userDto = createSampleUser(username, institution, roleName);
        var roles = userDto.getRoles();
        roles.forEach(this::addRoleToDb);
        identityService.addUser(userDto);

        return identityService.getUser(userDto);
    }

    private void addRoleToDb(RoleDto role) {
        try {
            identityService.addRole(role);
        } catch (InvalidInputException | InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        } catch (ConflictException e) {
            System.out.println("Role exists:" + role.toString());
        }
    }

    private UserDto createSampleUser(String username, URI institution, String roleName)
        throws InvalidEntryInternalException {
        return UserDto.newBuilder()
            .withRoles(createRoleList(roleName))
            .withInstitution(institution)
            .withUsername(username)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withViewingScope(randomViewingScope())
            .withCristinId(randomUri())
            .withFeideIdentifier(randomString())
            .withInstitutionCristinId(randomUri())
            .build();
    }

    private RoleDto createSampleRoleAndAddToDb(String roleName)
        throws InvalidEntryInternalException, ConflictException, InvalidInputException {
        RoleDto roleDto = createRole(roleName);
        identityService.addRole(roleDto);
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
