package no.unit.nva.useraccessmanagement.dao;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessmanagement.DynamoConfig.defaultDynamoConfigMapper;
import static no.unit.nva.useraccessmanagement.dao.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.useraccessmanagement.dao.UserDb.ERROR_DUE_TO_INVALID_ROLE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import no.unit.useraccessserivce.accessrights.AccessRight;
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

public class UserDbTest {

    public static final String SOME_USERNAME = "someUser";
    public static final String SOME_ROLENAME = "someRole";
    public static final String SOME_GIVEN_NAME = "givenName";
    public static final String SOME_FAMILY_NAME = "familyName";
    public static final String SOME_INSTITUTION = "SomeInstitution";
    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();
    public static final Javers JAVERS = JaversBuilder.javers().build();

    private UserDb userDao;
    private UserDb sampleUser;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        userDao = new UserDb();
        sampleUser = UserDb.newBuilder().withUsername(SOME_USERNAME).build();
    }

    @Test
    public void builderShouldSetTheHashKeyBasedOnUsername() throws InvalidEntryInternalException {
        sampleUser.setPrimaryHashKey("SomeOtherHashKey");
        String expectedHashKey = String.join(UserDb.FIELD_DELIMITER, UserDb.TYPE, SOME_USERNAME);
        assertThat(sampleUser.getPrimaryHashKey(), is(equalTo(expectedHashKey)));
    }

    @Test
    public void extractRolesDoesNotThrowExceptionWhenRolesAreValid()
        throws InvalidEntryInternalException, InvalidInputException {
        UserDb userWithValidRole = UserDb.fromUserDto(createUserWithRolesAndInstitution());
        Executable action = userWithValidRole::toUserDto;
        assertDoesNotThrow(action);
    }

    @Test
    public void userDbContainsListOfCristinUnitIdsThatShouldBeExcludedFromCuratorsView() throws BadRequestException {
        URI includedCristinUnit = randomUri();
        URI excludedCristinUnit = randomUri();
        ViewingScope scope = new ViewingScope(Set.of(includedCristinUnit), Set.of(excludedCristinUnit));
        UserDb userDb = UserDb.newBuilder().withUsername(randomString())
            .withViewingScope(scope)
            .build();
        assertThat(userDb.getViewingScope().getIncludedUnits(), containsInAnyOrder(includedCristinUnit));
        assertThat(userDb.getViewingScope().getExcludedUnits(), containsInAnyOrder(excludedCristinUnit));
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
        assertThat(userDao.getType(), is(equalTo(UserDb.TYPE)));
    }

    @Test
    void setTypeShouldNotChangeTheReturnedTypeValue() {
        userDao.setType("NotExpectedType");
        assertThat(userDao.getType(), is(equalTo(UserDb.TYPE)));
    }

    @Test
    void getHashKeyKeyShouldReturnTypeAndUsernameConcatenation() {
        String expectedHashKey = String.join(UserDb.FIELD_DELIMITER, UserDb.TYPE, SOME_USERNAME);
        assertThat(sampleUser.getPrimaryHashKey(), is(equalTo(expectedHashKey)));
    }

    @ParameterizedTest(name = "builder should throw exception when username is:\"{0}\"")
    @NullAndEmptySource
    void builderShouldThrowExceptionWhenUsernameIsNotValid(String invalidUsername) {
        Executable action = () -> UserDb.newBuilder()
            .withUsername(invalidUsername)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withInstitution(SOME_INSTITUTION)
            .withRoles(SAMPLE_ROLES)
            .build();

        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), containsString(UserDb.INVALID_USER_EMPTY_USERNAME));
    }

    @ParameterizedTest(name = "setUsername should throw exception when input is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "", "\n"})
    void setUsernameThrowsExceptionWhenUsernameIsNotValid(String invalidUsername) {
        UserDb userDb = new UserDb();
        assertThrows(InvalidEntryInternalException.class, () -> userDb.setUsername(invalidUsername));
    }

    @Test
    void serializationThrowsExceptionWhenUserHasNullUserName() {
        UserDb userDb = new UserDb();
        Executable action = () -> defaultDynamoConfigMapper.writeValueAsString(userDb);
        JsonMappingException thrownException = assertThrows(JsonMappingException.class, action);
        assertThat(thrownException.getCause(), is(instanceOf(InvalidEntryInternalException.class)));
    }

    @Test
    void shouldReturnCopyWithFilledInFields() throws InvalidEntryInternalException, BadRequestException {
        UserDb originalUser = randomUserDb();
        UserDb copy = originalUser.copy().build();
        assertThat(copy, is(equalTo(originalUser)));

        assertThat(copy, is(not(sameInstance(originalUser))));
    }

    @Test
    void shouldConvertToDtoAndBackWithoutInformationLoss() throws BadRequestException {
        UserDb originalUser = randomUserDb();
        UserDb converted = Try.of(originalUser)
            .map(UserDb::toUserDto)
            .map(UserDb::fromUserDto)
            .orElseThrow();
        Diff diff = JAVERS.compare(originalUser,converted);
        assertThat(diff.prettyPrint(),diff.hasChanges(),is(false));
        assertThat(converted, doesNotHaveEmptyValues());
        assertThat(originalUser, is(equalTo(converted)));
    }

    @ParameterizedTest(name = "fromUserDb throws Exception user contains invalidRole. Rolename:\"{0}\"")
    @NullAndEmptySource
    void fromUserDbThrowsExceptionWhenUserDbContainsInvalidRole(String invalidRoleName)
        throws InvalidEntryInternalException {
        RoleDb invalidRole = new RoleDb();
        invalidRole.setName(invalidRoleName);
        List<RoleDb> invalidRoles = Collections.singletonList(invalidRole);
        UserDb userDbWithInvalidRole = UserDb.newBuilder().withUsername(SOME_USERNAME).withRoles(invalidRoles).build();

        Executable action = userDbWithInvalidRole::toUserDto;
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(InvalidEntryInternalException.class)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void toUserDbThrowsExceptionWhenUserDbContainsInvalidRole(String invalidRoleName)
        throws InvalidEntryInternalException {
        RoleDto invalidRole = RoleDto.newBuilder().withName(SOME_ROLENAME).build();
        invalidRole.setRoleName(invalidRoleName);
        List<RoleDto> invalidRoles = Collections.singletonList(invalidRole);
        UserDto userWithInvalidRole = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(invalidRoles).build();

        Executable action = () -> UserDb.fromUserDto(userWithInvalidRole);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(InvalidEntryInternalException.class)));
    }

    @Test
    void roleValidationMethodLogsError()
        throws InvalidEntryInternalException {
        TestAppender appender = LogUtils.getTestingAppender(UserDb.class);
        RoleDto invalidRole = RoleDto.newBuilder().withName(SOME_ROLENAME).build();
        invalidRole.setRoleName(null);

        List<RoleDto> invalidRoles = Collections.singletonList(invalidRole);
        UserDto userWithInvalidRole = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(invalidRoles).build();

        Executable action = () -> UserDb.fromUserDto(userWithInvalidRole);
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
        throws InvalidEntryInternalException, BadRequestException {
        URI someCristinUnit = randomUri();
        URI someOtherCristinUnit = randomUri();
        Set<URI> visisbleUnits = Set.of(someCristinUnit, someOtherCristinUnit);
        ViewingScope scope = new ViewingScope(visisbleUnits, null);
        UserDb userDb = UserDb.newBuilder().withUsername(randomString())
            .withViewingScope(scope)
            .build();

        assertThat(userDb.getViewingScope().getIncludedUnits(),
                   containsInAnyOrder(someCristinUnit, someOtherCristinUnit));
    }

    private static List<RoleDb> createSampleRoles() {
        return Stream.of("Role1", "Role2")
            .map(attempt(UserDbTest::newRole))
            .map(Try::get)
            .collect(Collectors.toList());
    }

    private static RoleDb newRole(String str) throws InvalidEntryInternalException {
        return RoleDb.newBuilder().withName(str).build();
    }

    private UserDb randomUserDb() throws BadRequestException {
        UserDb randomUser = UserDb.newBuilder()
            .withUsername(randomString())
            .withFamilyName(randomString())
            .withGivenName(randomString())
            .withInstitution(randomString())
            .withRoles(randomRoles())
            .withViewingScope(randomViewingScope())
            .build();
        assertThat(randomUser, doesNotHaveEmptyValues());
        return randomUser;
    }

    private Collection<RoleDb> randomRoles() {
        return List.of(randomRole(), randomRole());
    }

    private RoleDb randomRole() {
        Set<AccessRight> accessRight = Set.of(randomElement(AccessRight.values()));
        return RoleDb.newBuilder().withName(randomString()).withAccessRights(accessRight).build();
    }

    private ViewingScope randomViewingScope() throws BadRequestException {
        return new ViewingScope(Set.of(randomUri()), Set.of(randomUri()));
    }

    private UserDto convertToUserDbAndBack(UserDto userDto) throws InvalidEntryInternalException {
        return UserDb.fromUserDto(userDto).toUserDto();
    }
}