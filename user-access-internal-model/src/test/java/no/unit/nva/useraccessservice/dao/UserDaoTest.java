package no.unit.nva.useraccessservice.dao;

import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.attempt.Try;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.dao.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.useraccessservice.dao.UserDao.ERROR_DUE_TO_INVALID_ROLE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserDaoTest {

    public static final String SOME_USERNAME = "someUser";
    public static final String SOME_GIVEN_NAME = "givenName";
    public static final String SOME_FAMILY_NAME = "familyName";

    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();
    private static final Javers JAVERS = JaversBuilder.javers().build();

    private UserDao userDao;
    private UserDao sampleUser;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        userDao = new UserDao();
        sampleUser = UserDao.newBuilder().withUsername(SOME_USERNAME).build();
    }

    @Test
    void builderShouldSetTheHashKeyBasedOnUsername() throws InvalidEntryInternalException {
        sampleUser.setPrimaryKeyHashKey("SomeOtherHashKey");
        String expectedHashKey = String.join(UserDao.FIELD_DELIMITER, UserDao.TYPE_VALUE, SOME_USERNAME);
        assertThat(sampleUser.getPrimaryKeyHashKey(), is(equalTo(expectedHashKey)));
    }

    @Test
    void extractRolesDoesNotThrowExceptionWhenRolesAreValid()
            throws InvalidEntryInternalException {
        UserDao userWithValidRole = UserDao.fromUserDto(createUserWithRolesAndInstitution());
        Executable action = userWithValidRole::toUserDto;
        assertDoesNotThrow(action);
    }

    @Test
    void userDbContainsListOfCristinUnitIdsThatShouldBeExcludedFromCuratorsView() {

        var includedCristinUnit = randomCristinOrgId();
        var excludedCristinUnit = randomCristinOrgId();
        ViewingScopeDb scope = new ViewingScopeDb(Set.of(includedCristinUnit),
                Set.of(excludedCristinUnit));
        UserDao userDao = UserDao.newBuilder().withUsername(randomString())
                .withViewingScope(scope)
                .build();
        assertThat(userDao.getViewingScope().getIncludedUnits(), containsInAnyOrder(includedCristinUnit));
        assertThat(userDao.getViewingScope().getExcludedUnits(), containsInAnyOrder(excludedCristinUnit));
    }

    @Test
    void setUsernameShouldAddUsernameToUserObject() {
        userDao.setUsername(SOME_USERNAME);
        assertThat(userDao.getUsername(), is(equalTo(SOME_USERNAME)));
    }

    @Test
    void getUsernameShouldGetTheSetUsernameToUserObject() {
        assertThat(userDao.getUsername(), is(nullValue()));
        userDao.setUsername(SOME_USERNAME);
        assertThat(userDao.getUsername(), is(equalTo(SOME_USERNAME)));
    }

    @Test
    void getTypeShouldReturnConstantTypeValue() {
        assertThat(userDao.getType(), is(equalTo(UserDao.TYPE_VALUE)));
    }

    @Test
    void setTypeShouldNotAcceptWrongTypeValues() {
        String illegalType = "NotExpectedType";
        var exception = assertThrows(BadRequestException.class, () -> userDao.setType(illegalType));
        assertThat(exception.getMessage(), allOf(containsString(illegalType), containsString(UserDao.TYPE_VALUE)));
    }

    @Test
    void getHashKeyKeyShouldReturnTypeAndUsernameConcatenation() {
        String expectedHashKey = String.join(UserDao.FIELD_DELIMITER, UserDao.TYPE_VALUE, SOME_USERNAME);
        assertThat(sampleUser.getPrimaryKeyHashKey(), is(equalTo(expectedHashKey)));
    }

    @ParameterizedTest(name = "builder should throw exception when username is:\"{0}\"")
    @NullAndEmptySource
    void builderShouldThrowExceptionWhenUsernameIsNotValid(String invalidUsername) {
        Executable action = () -> UserDao.newBuilder()
                .withUsername(invalidUsername)
                .withGivenName(SOME_GIVEN_NAME)
                .withFamilyName(SOME_FAMILY_NAME)
                .withInstitution(SOME_INSTITUTION)
                .withRoles(SAMPLE_ROLES)
                .build();

        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), containsString(UserDao.INVALID_USER_EMPTY_USERNAME));
    }

    @ParameterizedTest(name = "setUsername should throw exception when input is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "", "\n"})
    void setUsernameThrowsExceptionWhenUsernameIsNotValid(String invalidUsername) {
        UserDao userDao = new UserDao();
        assertThrows(InvalidEntryInternalException.class, () -> userDao.setUsername(invalidUsername));
    }

    @Test
    void shouldReturnCopyWithFilledInFields() throws InvalidEntryInternalException {
        UserDao originalUser = randomUserDb();
        UserDao copy = originalUser.copy().build();
        assertThat(copy, is(equalTo(originalUser)));

        assertThat(copy, is(not(sameInstance(originalUser))));
    }

    @Test
    void shouldConvertToDtoAndBackWithoutInformationLoss() {
        UserDao originalUser = randomUserDb();
        UserDao converted = Try.of(originalUser)
                .map(UserDao::toUserDto)
                .map(UserDao::fromUserDto)
                .orElseThrow();

        assertThat(originalUser, is(equalTo(converted)));
        Diff diff = JAVERS.compare(originalUser, converted);
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(converted, doesNotHaveEmptyValues());
    }

//    @ParameterizedTest(name = "fromUserDb throws Exception user contains invalidRole. Rolename:\"{0}\"")
//    @NullAndEmptySource
//    void fromUserDbThrowsExceptionWhenUserDbContainsInvalidRole(String invalidRoleName)
//        throws InvalidEntryInternalException {
//        RoleDb invalidRole = new RoleDb();
//        invalidRole.setName(invalidRoleName);
//        List<RoleDb> invalidRoles = Collections.singletonList(invalidRole);
//        UserDao userDaoWithInvalidRole = UserDao.newBuilder()
//            .withUsername(SOME_USERNAME)
//            .withRoles(invalidRoles)
//            .build();
//
//        Executable action = userDaoWithInvalidRole::toUserDto;
//        RuntimeException exception = assertThrows(RuntimeException.class, action);
//        assertThat(exception.getCause(), is(instanceOf(InvalidEntryInternalException.class)));
//    }

//    @ParameterizedTest
//    @NullAndEmptySource
//    void toUserDbThrowsExceptionWhenUserDbContainsInvalidRole(String invalidRoleName)
//        throws InvalidEntryInternalException {
//        RoleDto invalidRole = RoleDto.newBuilder().withRoleName(SOME_ROLENAME).build();
//        invalidRole.setRoleName(invalidRoleName);
//        List<RoleDto> invalidRoles = Collections.singletonList(invalidRole);
//        UserDto userWithInvalidRole = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(invalidRoles).build();
//
//        Executable action = () -> UserDao.fromUserDto(userWithInvalidRole);
//        RuntimeException exception = assertThrows(RuntimeException.class, action);
//        assertThat(exception.getCause(), is(instanceOf(InvalidEntryInternalException.class)));
//    }

    @Test
    void roleValidationMethodLogsError()
            throws InvalidEntryInternalException {
        TestAppender appender = LogUtils.getTestingAppender(UserDao.class);
        RoleDto invalidRole = RoleDto.newBuilder().withRoleName(randomRoleName()).build();
        invalidRole.setRoleName(null);

        List<RoleDto> invalidRoles = Collections.singletonList(invalidRole);
        UserDto userWithInvalidRole = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(invalidRoles).build();

        Executable action = () -> UserDao.fromUserDto(userWithInvalidRole);
        assertThrows(RuntimeException.class, action);

        assertThat(appender.getMessages(), StringContains.containsString(ERROR_DUE_TO_INVALID_ROLE));
    }

    @Test
    void toUserDbReturnsValidUserDbWhenUserDtoIsValid() throws InvalidEntryInternalException {
        UserDto userOnlyWithOnlyUsername = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        UserDto actualUserOnlyWithName = convertToUserDbAndBack(userOnlyWithOnlyUsername);
        assertThat(actualUserOnlyWithName, is(equalTo(userOnlyWithOnlyUsername)));
    }

    @Test
    void shouldContainListOfCristinOrganizationIdsThatDefineCuratorsScope()
            throws InvalidEntryInternalException {
        URI someCristinUnit = randomCristinOrgId();
        URI someOtherCristinUnit = randomCristinOrgId();
        Set<URI> visisbleUnits = Set.of(someCristinUnit, someOtherCristinUnit);

        ViewingScopeDb scope = new ViewingScopeDb(visisbleUnits, null);
        UserDao userDao = UserDao.newBuilder().withUsername(randomString())
                .withViewingScope(scope)
                .build();

        assertThat(userDao.getViewingScope().getIncludedUnits(),
                containsInAnyOrder(someCristinUnit, someOtherCristinUnit));
    }

    @Test
    void shouldContainInformationThatAllowsLocatingUserBasedOnFeideAndCristinInformationOfAssociatedPerson() {
        var feideIdentifier = randomString();
        var personCristinId = randomUri();
        var orgCristinId = randomUri();
        var dao = UserDao.newBuilder().withUsername(randomString())
                .withCristinId(personCristinId)
                .withFamilyName(randomString())
                .withGivenName(randomString())
                .withInstitution(randomUri())
                .withFeideIdentifier(feideIdentifier)
                .withInstitutionCristinId(orgCristinId)
                .build();

        assertThat(dao.getCristinId(), is(equalTo(personCristinId)));
        assertThat(dao.getFeideIdentifier(), is(equalTo(feideIdentifier)));
        assertThat(dao.getInstitutionCristinId(), is(equalTo(orgCristinId)));
    }

    @Test
    void shouldCopyWithoutInformationLoss() {
        var source = randomUserDb();
        assertThat(source, doesNotHaveEmptyValues());
        var copy = source.copy().build();
        assertThat(copy, doesNotHaveEmptyValues());
        assertThat(copy, is(equalTo(source)));
    }

    private static List<RoleDb> createSampleRoles() {
        return Stream.of(randomRoleName(), randomRoleName())
                .map(attempt(UserDaoTest::newRole))
                .map(Try::get)
                .collect(Collectors.toList());
    }

    private static RoleDb newRole(RoleName roleName) throws InvalidEntryInternalException {
        return RoleDb.newBuilder().withName(roleName).build();
    }

    private UserDao randomUserDb() {
        UserDao randomUser = UserDao.newBuilder()
                .withUsername(randomString())
                .withFamilyName(randomString())
                .withGivenName(randomString())
                .withInstitution(randomCristinOrgId())
                .withRoles(randomRoles())
                .withViewingScope(ViewingScopeDb.fromViewingScope(randomViewingScope()))
                .withCristinId(randomUri())
                .withInstitutionCristinId(randomCristinOrgId())
                .withFeideIdentifier(randomString())
                .withAffiliation(randomCristinOrgId())
                .build();
        assertThat(randomUser, doesNotHaveEmptyValues());
        return randomUser;
    }

    private Collection<RoleDb> randomRoles() {
        return List.of(randomRole(), randomRole());
    }

    private RoleDb randomRole() {
        Set<AccessRight> accessRight = Set.of(randomElement(AccessRight.values()));
        return RoleDb.newBuilder().withName(randomRoleName()).withAccessRights(accessRight).build();
    }

    private UserDto convertToUserDbAndBack(UserDto userDto) throws InvalidEntryInternalException {
        return UserDao.fromUserDto(userDto).toUserDto();
    }
}