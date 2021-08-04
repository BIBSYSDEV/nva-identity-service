package no.unit.nva.useraccessmanagement.dao;

import static no.unit.nva.useraccessmanagement.dao.EntityUtils.createUserWithRolesAndInstitution;
import static no.unit.nva.useraccessmanagement.dao.UserDb.ERROR_DUE_TO_INVALID_ROLE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.useraccessmanagement.dao.UserDb.Builder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.attempt.Try;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class UserDbTest {

    public static final String SOME_USERNAME = "someUser";
    public static final String SOME_ROLENAME = "someRole";
    public static final String SOME_GIVEN_NAME = "givenName";
    public static final String SOME_FAMILY_NAME = "familyName";
    public static final String SOME_INSTITUTION = "SomeInstitution";
    public static final List<RoleDb> SAMPLE_ROLES = createSampleRoles();

    private UserDb dynamoFunctionalityTestUser;
    private UserDb sampleUser;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        dynamoFunctionalityTestUser = new UserDb();
        sampleUser = UserDb.newBuilder().withUsername(SOME_USERNAME).build();
    }

    @Test
    public void builderShouldSetTheHashKeyBasedOnUsername() throws InvalidEntryInternalException {
        sampleUser.setPrimaryHashKey("SomeOtherHashKey");
        String expectedHashKey = String.join(UserDb.FIELD_DELIMITER, UserDb.TYPE, SOME_USERNAME);
        assertThat(sampleUser.getPrimaryHashKey(), is(equalTo(expectedHashKey)));
    }

    @Test
    public void extractRolesDoesNotThrowExceptionWhenRolesAreValid() throws InvalidEntryInternalException {
        UserDb userWithValidRole = UserDb.fromUserDto(createUserWithRolesAndInstitution());
        Executable action = userWithValidRole::toUserDto;
        assertDoesNotThrow(action);
    }

    @Test
    void userDbHasABuilder() {
        Builder builder = UserDb.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void setUsernameShouldAddUsernameToUserObject() {
        dynamoFunctionalityTestUser.setUsername(SOME_USERNAME);
        assertThat(dynamoFunctionalityTestUser.getUsername(), is(equalTo(SOME_USERNAME)));
    }

    @Test
    void getUsernameShouldGetTheSetUsernameToUserObject() {
        assertThat(dynamoFunctionalityTestUser.getUsername(), is(nullValue()));

        dynamoFunctionalityTestUser.setUsername(SOME_USERNAME);
        assertThat(dynamoFunctionalityTestUser.getUsername(), is(equalTo(SOME_USERNAME)));
    }

    @Test
    void getTypeShouldReturnConstantTypeValue() {
        assertThat(dynamoFunctionalityTestUser.getType(), is(equalTo(UserDb.TYPE)));
    }

    @Test
    void setTypeShouldNotChangeTheReturnedTypeValue() {
        dynamoFunctionalityTestUser.setType("NotExpectedType");
        assertThat(dynamoFunctionalityTestUser.getType(), is(equalTo(UserDb.TYPE)));
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

    @Test
    void copyShouldReturnBuilderWithFilledInFields() throws InvalidEntryInternalException {
        UserDb originalUser = UserDb.newBuilder()
            .withUsername(SOME_USERNAME)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withInstitution(SOME_INSTITUTION)
            .withRoles(SAMPLE_ROLES)
            .build();
        UserDb copy = originalUser.copy().build();
        assertThat(copy, is(equalTo(originalUser)));

        assertThat(copy, is(not(sameInstance(originalUser))));
    }

    @Test
    void setPrimaryHashKeyThrowsExceptionWhenKeyDoesNotStartWithType() {
        UserDb userDb = new UserDb();
        Executable action = () -> userDb.setPrimaryHashKey("SomeKey");
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), containsString(UserDb.INVALID_PRIMARY_HASH_KEY));
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

    private static List<RoleDb> createSampleRoles() {
        return Stream.of("Role1", "Role2")
            .map(attempt(UserDbTest::newRole))
            .map(Try::get)
            .collect(Collectors.toList());
    }

    private static RoleDb newRole(String str) throws InvalidEntryInternalException {
        return RoleDb.newBuilder().withName(str).build();
    }

    private UserDto convertToUserDbAndBack(UserDto userDto) throws InvalidEntryInternalException {
        return UserDb.fromUserDto(userDto).toUserDto();
    }
}