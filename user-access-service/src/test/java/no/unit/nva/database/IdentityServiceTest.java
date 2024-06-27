package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.database.EntityUtils.createRole;
import static no.unit.nva.database.EntityUtils.randomRoleName;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityService.Constants;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.testutils.RandomDataGenerator;
import no.unit.nva.useraccessservice.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.dao.UserDao;
import no.unit.nva.useraccessservice.dao.ViewingScopeDb;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
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

class IdentityServiceTest extends LocalIdentityService {

    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();
    private static final String SOME_USERNAME = "someusername";
    private static final String SOME_OTHER_USERNAME = randomString();
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    private static final URI SOME_INSTITUTION = randomCristinOrgId();
    private static final URI SOME_OTHER_INSTITUTION = randomCristinOrgId();
    private IdentityService identityService;
    private DynamoDbTable<RoleDb> rolesTable;
    private DynamoDbTable<UserDao> usersTable;

    @BeforeEach
    public void init() {
        identityService = createDatabaseServiceUsingLocalStorage();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(localDynamo).build();
        rolesTable = enhancedClient.table(Constants.USERS_AND_ROLES_TABLE, RoleDb.TABLE_SCHEMA);
        usersTable = enhancedClient.table(Constants.USERS_AND_ROLES_TABLE, UserDao.TABLE_SCHEMA);
    }

    @DisplayName("getRole() throws NotFoundException when the role-name does not exist in the database")
    @Test
    void databaseServiceThrowsNotFoundExceptionWhenRoleNameDoesNotExist() throws InvalidEntryInternalException {
        RoleDto queryObject = createRole(EntityUtils.randomRoleName());
        Executable action = () -> identityService.getRole(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @DisplayName("addRole() inserts valid role")
    @Test
    void addRoleInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, NotFoundException, InvalidInputException, ConflictException {
        RoleDto insertedUser = createSampleRoleAndAddToDb(randomRoleName());
        RoleDto savedUser = identityService.getRole(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addRole() throws Exception when trying to save role without name")
    @Test
    void addRoleShouldNotSaveUserWithoutUsername() throws InvalidEntryInternalException {
        RoleDto illegalRole = createIllegalRole();
        Executable illegalAction = () -> identityService.addRole(illegalRole);
        InvalidInputException exception = assertThrows(InvalidInputException.class, illegalAction);
        assertThat(exception.getMessage(), containsString(RoleDto.MISSING_ROLE_NAME_ERROR));
    }

    @DisplayName("addRole() throws ConflictException when trying to save user with existing username")
    @Test
    void addRoleThrowsConflictExceptionWhenTryingToSaveAlreadyExistingUser()
        throws InvalidEntryInternalException, InvalidInputException, ConflictException {

        var conflictingRoleName = EntityUtils.randomRoleName();
        createSampleRoleAndAddToDb(conflictingRoleName);

        RoleDto conflictingRole = createRole(conflictingRoleName);

        Executable action = () -> identityService.addRole(conflictingRole);
        ConflictException exception = assertThrows(ConflictException.class, action);
        assertThat(exception.getMessage(), containsString(ROLE_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    @DisplayName("getUser() returns non empty user when username exists in database")
    @Test
    void databaseServiceReturnsNonEmptyUserWhenUsernameExistsInDatabase()
        throws InvalidEntryInternalException, ConflictException, NotFoundException, InvalidInputException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        UserDto savedUser = identityService.getUser(insertedUser);

        assertThat(insertedUser, doesNotHaveEmptyValues());
        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("getUser() throws NotFoundException when the username does exist in the database")
    @Test
    void databaseServiceThrowsNotFoundExceptionWhenUsernameDoesNotExist()
        throws InvalidEntryInternalException {
        UserDto queryObject = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        Executable action = () -> identityService.getUser(queryObject);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @DisplayName("addUser() saves user with institution without roles")
    @Test
    void addUserShouldSaveUserWithoutRoles()
        throws InvalidEntryInternalException, ConflictException, NotFoundException, InvalidInputException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, null);
        UserDto actualUser = identityService.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("addUser() throws ConflictException when trying to save user with existing username")
    @Test
    void addUserThrowsConflictExceptionWhenTryingToSaveAlreadyExistingUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {

        String conflictingUsername = SOME_USERNAME;
        createSampleUserAndAddUserToDb(conflictingUsername, SOME_INSTITUTION, randomRoleName());

        UserDto conflictingUser = createUserWithRole(conflictingUsername,
                                                     SOME_OTHER_INSTITUTION, createRole(randomRoleName()));

        Executable action = () -> identityService.addUser(conflictingUser);
        ConflictException exception = assertThrows(ConflictException.class, action);
        assertThat(exception.getMessage(), containsString(USER_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    @Test
    void addUserAddsCurrentlySavedVersionOfRoleInNewUser()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        RoleDto existingRole = createSampleRoleAndAddToDb(randomRoleName());
        assertThat(existingRole, doesNotHaveEmptyValues());

        UserDto userWithoutRoleDetails = createUserWithRoleReference(existingRole);
        identityService.addUser(userWithoutRoleDetails);
        UserDto savedUser = identityService.getUser(userWithoutRoleDetails);
        RoleDto actualRole = savedUser.getRoles().stream().collect(SingletonCollector.collect());
        assertThat(actualRole, is(equalTo(existingRole)));
    }

    @DisplayName("updateUser() updates existing user with input user when input user is valid")
    @Test
    void updateUserUpdatesExistingUserWithInputUserWhenInputUserIsValid()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException,
               BadRequestException {
        UserDto existingUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        UserDto expectedUser = cloneAndAddNewRoleAndViewingScope(existingUser);

        identityService.updateUser(expectedUser);
        UserDto actualUser = identityService.getUser(expectedUser);
        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser, is(not(sameInstance(expectedUser))));
    }

    @DisplayName("updateUser() throws NotFoundException when the input username does not exist")
    @Test
    void updateUserThrowsNotFoundExceptionWhenTheInputUsernameDoesNotExist()
        throws InvalidEntryInternalException {
        UserDto userUpdate = createSampleUser(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        Executable action = () -> identityService.updateUser(userUpdate);
        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(USER_NOT_FOUND_MESSAGE));
    }

    @DisplayName("updateUser() throws Exception when the input is invalid ")
    @Test
    void updateUserThrowsExceptionWhenTheInputIsInvalid()
        throws ConflictException, InvalidEntryInternalException,
               NotFoundException, InvalidInputException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        UserDto invalidUser = new UserDto();
        Executable action = () -> identityService.updateUser(invalidUser);
        assertThrows(RuntimeException.class, action);
    }

    @Test
    void listUsersByInstitutionReturnsAllUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException,
               NotFoundException, InvalidInputException {
        UserDto someUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        UserDto someOtherUser = createSampleUserAndAddUserToDb(SOME_OTHER_USERNAME, SOME_INSTITUTION, randomRoleName());
        List<UserDto> queryResult = identityService.listUsers(SOME_INSTITUTION);
        assertThat(queryResult, containsInAnyOrder(someUser, someOtherUser));
    }

    @Test
    void listUsersByInstitutionReturnsEmptyListWhenThereAreNoUsersForSpecifiedInstitution()
        throws ConflictException, InvalidEntryInternalException, NotFoundException, InvalidInputException {
        createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        List<UserDto> queryResult = identityService.listUsers(SOME_OTHER_INSTITUTION);
        assertThat(queryResult, is(empty()));
    }

    @Test
    void roleDbWithAccessRightsIsSavedInDatabase() throws InvalidEntryInternalException {
        var accessRights = Set.of(AccessRight.MANAGE_DOI, AccessRight.MANAGE_CUSTOMERS);

        RoleDb roleWithAccessRights = RoleDb.newBuilder()
                                          .withAccessRights(accessRights)
                                          .withName(randomRoleName())
                                          .build();

        rolesTable.putItem(roleWithAccessRights);

        var savedRole = fetchRoleDirectlyFromTable(roleWithAccessRights);
        assertThat(savedRole, is(equalTo(roleWithAccessRights)));
    }

    @DisplayName("getRole() returns non empty role when role-name exists in database")
    @Test
    void databaseServiceReturnsNonEmptyRoleWhenRoleNameExistsInDatabase()
        throws InvalidEntryInternalException, NotFoundException, InvalidInputException, ConflictException {
        RoleDto insertedRole = createSampleRoleAndAddToDb(randomRoleName());
        RoleDto savedRole = identityService.getRole(insertedRole);

        assertThat(insertedRole, doesNotHaveEmptyValues());
        assertThat(savedRole, is(equalTo(insertedRole)));
    }

    @Test
    void addUserInsertsValidItemInDatabase()
        throws InvalidEntryInternalException, ConflictException, NotFoundException, InvalidInputException {
        UserDto insertedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, SOME_INSTITUTION, randomRoleName());
        assertThat(insertedUser, doesNotHaveEmptyValues());
        UserDto savedUser = identityService.getUser(insertedUser);

        assertThat(savedUser, is(equalTo(insertedUser)));
    }

    @DisplayName("addUser() saves user with roles and without institution")
    @Test
    void addUserSavesAUserWithoutInstitution() throws InvalidEntryInternalException, ConflictException,
                                                      NotFoundException, InvalidInputException {
        UserDto expectedUser = createSampleUserAndAddUserToDb(SOME_USERNAME, null, randomRoleName());
        UserDto actualUser = identityService.getUser(expectedUser);

        assertThat(actualUser, is(equalTo(expectedUser)));
        assertThat(actualUser.getInstitution(), is(equalTo(null)));
    }

    @Test
    void addUserDoesNotAddNonExistingRolesInCreatedUser() throws ConflictException, NotFoundException {
        UserDto userWithNonExistingRole = createUserWithRole(SOME_USERNAME, SOME_INSTITUTION,
                                                             createRole(randomRoleName()));
        identityService.addUser(userWithNonExistingRole);

        UserDto actualUser = identityService.getUser(userWithNonExistingRole);
        UserDto expectedUser = userWithNonExistingRole.copy().withRoles(Collections.emptyList()).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @DisplayName("updateUser() updates existing user with input user when input user is valid")
    @Test
    void updateUserUpdatesAssignsCorrectVersionOfRoleInUser()
        throws ConflictException, NotFoundException, InvalidInputException {
        RoleDto existingRole = createRole(randomRoleName());
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
                                   .withAffiliation(randomCristinOrgId())
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
        RoleDto nonExistingRole = EntityUtils.createRole(EntityUtils.randomRoleName());
        attempt(() -> databaseService.getRole(nonExistingRole));
        assertThat(testAppender.getMessages(),
                   StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @Test
    void getUserReturnsUserWhenUserHasEmptyValuesForCollections()
        throws ConflictException, NotFoundException {
        var user = UserDto.newBuilder().withUsername(randomString()).build();
        databaseService.addUser(user);
        var retrievedUser = databaseService.getUser(user);
        assertThat(retrievedUser, is(equalTo(user)));
    }

    @Test
    void shouldReturnPageOfResultsWhenScanDatabaseRequestIsSubmitted()
        throws ConflictException, NotFoundException, InvalidInputException {
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
    void shouldFetchAllUsersOfPersonBasedOnCristinPersonIdentifier()
        throws ConflictException, NotFoundException, InvalidInputException {
        var cristinPersonId = randomUri();
        var user1 = createUserAndAddUserToDb(cristinPersonId, randomCristinOrgId(), randomString());
        var user2 = createUserAndAddUserToDb(cristinPersonId, randomCristinOrgId(), randomString());
        var retrievedUsers = identityService.getUsersByCristinId(cristinPersonId);
        assertThat(retrievedUsers, containsInAnyOrder(user1, user2));
    }

    @Test
    void shouldFetchUsersOfPersonBasedOnCristinPersonIdAndCristinOrgId()
        throws ConflictException, NotFoundException, InvalidInputException {
        var cirstinPersonId = randomUri();
        var cristinOrgId = randomCristinOrgId();
        var expectedUser = createUserAndAddUserToDb(cirstinPersonId, cristinOrgId, randomString());
        var retrievedUser = identityService.getUserByPersonCristinIdAndCustomerCristinId(cirstinPersonId, cristinOrgId);
        assertThat(retrievedUser, is(equalTo(expectedUser)));
    }

    @Test
    void addClientInsertsValidItemInDatabase() throws InvalidEntryInternalException, NotFoundException {
        var expectedClient = ClientDto
                                 .newBuilder()
                                 .withClientId(RandomDataGenerator.randomString())
                                 .withCustomer(RandomDataGenerator.randomUri())
                                 .build();

        identityService.addExternalClient(expectedClient);
        var insertedClient = databaseService.getClient(expectedClient);

        assertThat(insertedClient, is(equalTo(expectedClient)));
    }

    @Test
    void getExternalClientReturnsTheItemFromDatabase() throws InvalidEntryInternalException, NotFoundException {
        var expectedClient = ClientDto
                                 .newBuilder()
                                 .withClientId(RandomDataGenerator.randomString())
                                 .withCustomer(RandomDataGenerator.randomUri())
                                 .build();

        databaseService.addExternalClient(expectedClient);
        var databaseClient = identityService.getClient(expectedClient);

        assertThat(databaseClient, is(equalTo(expectedClient)));
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
                   .withName(randomRoleName())
                   .withAccessRights(Set.of(randomElement(AccessRight.values())))
                   .build();
    }

    private UserDto createUserAndAddUserToDb(URI cristinId, URI cristinOrgId, String feideIdentifier)
        throws ConflictException, NotFoundException, InvalidInputException {
        var roles = createSampleRoles().stream().map(RoleDb::toRoleDto).collect(Collectors.toList());
        for (var role : roles) {
            identityService.addRole(role);
        }
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
                       .withAffiliation(randomCristinOrgId())
                       .build();
        identityService.addUser(user);
        var savedUser = identityService.getUser(user);
        assertThat(user, doesNotHaveEmptyValues());
        assertThat(savedUser, doesNotHaveEmptyValues());
        return user;
    }

    private void createSampleUsers(int numberOfUsers)
        throws ConflictException, NotFoundException, InvalidInputException {
        for (int counter = 0; counter < numberOfUsers; counter++) {
            createSampleUserAndAddUserToDb(randomString(), randomUri(), randomRoleName());
        }
    }

    private List<UserDto> scanDatabaseDirectlyAndGetAllUsersInExpectedOrderIgnoringRoleEntries(int pageSize) {
        return localDynamo.scan(ScanRequest.builder().tableName(Constants.USERS_AND_ROLES_TABLE).scanFilter(
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
        throws InvalidEntryInternalException {
        RoleDto roleWithoutDetails = RoleDto.newBuilder().withRoleName(existingRole.getRoleName()).build();
        return createSampleUser(SOME_USERNAME, SOME_INSTITUTION, randomRoleName())
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
        RoleDto roleWithOnlyRolename =
            RoleDto.newBuilder().withRoleName(existingUser.getRoles().stream().findFirst().orElseThrow().getRoleName()).build();
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

    private RoleDto createIllegalRole() throws InvalidEntryInternalException {
        RoleDto illegalRole = createRole(randomRoleName());
        illegalRole.setRoleName(null);
        return illegalRole;
    }

    private UserDto cloneAndAddNewRoleAndViewingScope(UserDto existingUser)
        throws InvalidEntryInternalException, InvalidInputException, ConflictException, BadRequestException {
        var someOtherRole = createRole(randomRoleName());
        var viewingScope = ViewingScope.create(Set.of(randomCristinOrgId()), Set.of(randomCristinOrgId()));

        identityService.addRole(someOtherRole);
        return existingUser.copy().withRoles(Collections.singletonList(someOtherRole))
                   .withViewingScope(viewingScope)
                   .build();
    }

    private UserDto createSampleUserAndAddUserToDb(String username, URI institution, RoleName roleName)
        throws InvalidEntryInternalException, ConflictException,
               NotFoundException, InvalidInputException {
        var userDto = createSampleUser(username, institution, roleName);
        var roles = userDto.getRoles();
        addRolesIfTheyDoNotExist(roles);
        identityService.addUser(userDto);

        return identityService.getUser(userDto);
    }

    private void addRolesIfTheyDoNotExist(Collection<RoleDto> roles) throws InvalidInputException, ConflictException {
        for (var role : roles) {
            var fetchRole = attempt(() -> identityService.getRole(role));
            if (fetchRole.isFailure()) {
                identityService.addRole(role);
            }
        }
    }

    private UserDto createSampleUser(String username, URI institution, RoleName roleName)
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
                   .withInstitutionCristinId(randomCristinOrgId())
                   .withAffiliation(randomCristinOrgId())
                   .build();
    }

    private RoleDto createSampleRoleAndAddToDb(RoleName roleName)
        throws InvalidEntryInternalException, InvalidInputException, ConflictException {
        RoleDto roleDto = createRole(roleName);
        identityService.addRole(roleDto);

        return roleDto;
    }

    private List<RoleDto> createRoleList(RoleName rolename) throws InvalidEntryInternalException {
        if (nonNull(rolename)) {
            RoleDto roleDto = createRole(rolename);
            return Collections.singletonList(roleDto);
        } else {
            return Collections.emptyList();
        }
    }
}
