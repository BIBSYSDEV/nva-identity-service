package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SOME_ROLENAME;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SOME_USERNAME;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.createRole;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.createUserWithRoleWithoutInstitution;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.createUserWithRolesAndInstitutionAndViewingScope;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.UserDto.Builder;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserDtoTest extends DtoTest {

    public static final Set<RoleDto> sampleRoles = createSampleRoles();

    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final String SOME_OTHER_ROLENAME = randomString();

    protected static final String USER_TYPE_LITERAL = "User";
    private static final String FIRST_ACCESS_RIGHT = "ApproveDoi";
    private static final String SECOND_ACCESS_RIGHT = "RejectDoi";
    private static final JSON defaultRestObjectMapper = JSON.std;

    @DisplayName("UserDto object contains type with value \"User\"")
    @Test
    void userDtoSerializedObjectContainsTypeWithValueUser() throws IOException {
        UserDto sampleUser = createUserWithRolesAndInstitutionAndViewingScope();
        var jsonMap = toMap(sampleUser);

        String actualType = jsonMap.get(JSON_TYPE_ATTRIBUTE).toString();
        assertThat(actualType, is(equalTo(USER_TYPE_LITERAL)));
    }

    @DisplayName("UserDto cannot be created without type value")
    @Test
    @Disabled("We have no technical ability with Jackson-jr (2.13.1) to impose setting the type")
    void userDtoCannotBeCreatedWithoutTypeValue() throws IOException {
        UserDto sampleUser = createUserWithRolesAndInstitutionAndViewingScope();
        var jsonMap = toMap(sampleUser);
        jsonMap.remove(JSON_TYPE_ATTRIBUTE);
        String jsonStringWithoutType = JSON.std.asString(jsonMap);
        Executable action = () -> JSON.std.beanFrom(UserDto.class, jsonStringWithoutType);
        InvalidTypeIdException exception = assertThrows(InvalidTypeIdException.class, action);
        assertThat(exception.getMessage(), containsString(UserDto.TYPE));
    }

    @DisplayName("UserDto can be created when it contains the right type value")
    @Test
    void userDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
        throws InvalidEntryInternalException, IOException {
        UserDto sampleUser = createUserWithRolesAndInstitutionAndViewingScope();
        var json = toMap(sampleUser);
        assertThatSerializedItemContainsType(json, USER_TYPE_LITERAL);

        String jsonStringWithType = defaultRestObjectMapper.asString(json);
        UserDto deserializedItem = defaultRestObjectMapper.beanFrom(UserDto.class, jsonStringWithType);

        assertThat(deserializedItem, is(equalTo(sampleUser)));
        assertThat(deserializedItem, is(not(sameInstance(sampleUser))));
    }

    @Test
    void getAccessRightsReturnsAccessRightsWithoutDuplicates() {
        final UserDto user = createUserWithRoleWithoutInstitution();
        final Set<String> expectedAccessRights = new HashSet<>(user.getAccessRights());
        List<RoleDto> newRoles = duplicateRoles(user);
        UserDto newUser = user.copy().withRoles(newRoles).build();

        HashSet<String> actualAccessRights = new HashSet<>(newUser.getAccessRights());
        assertThat(actualAccessRights, is(equalTo(expectedAccessRights)));
    }

    @Test
    void getAccessRightsReturnsAllAccessRightsContainedInTheUsersRoles() {

        RoleDto firstRole = sampleRole(FIRST_ACCESS_RIGHT, SOME_ROLENAME);
        RoleDto secondRole = sampleRole(SECOND_ACCESS_RIGHT, SOME_OTHER_ROLENAME);

        List<RoleDto> roles = List.of(firstRole, secondRole);
        UserDto user = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(roles).build();

        Set<String> expectedAccessRights = Set.of(FIRST_ACCESS_RIGHT, SECOND_ACCESS_RIGHT);
        assertThat(user.getAccessRights(), containsInAnyOrder(expectedAccessRights.toArray(String[]::new)));
    }

    @Test
    void getRolesReturnsEmptyListWhenRolesIsNull() {
        UserDto userDto = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(null).build();
        var roles = userDto.getRoles();
        assertThat(roles, is(not(nullValue())));
        assertThat(roles, is(empty()));
    }

    @Test
    void userDtoHasAConstructorWithoutArgs() {
        new UserDto();
    }

    @Test
    void userDtoHasABuilder() {
        Builder builder = UserDto.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void builderReturnsUserDtoWhenInstitutionIsEmpty() {
        UserDto user = createUserWithRoleWithoutInstitution();
        assertThat(user.getUsername(), is(equalTo(SOME_USERNAME)));
        assertThat(user.getRoles(), containsInAnyOrder(sampleRoles.toArray(RoleDto[]::new)));
        assertThat(user.getInstitution(), is(equalTo(null)));
    }

    @Test
    void builderReturnsUserDtoWhenIRolesIsEmpty() {
        UserDto user = UserDto.newBuilder().withUsername(SOME_USERNAME)
            .withInstitution(SOME_INSTITUTION).build();
        assertThat(user.getUsername(), is(equalTo(SOME_USERNAME)));
        assertThat(user.getRoles(), is(empty()));
        assertThat(user.getInstitution(), is(equalTo(SOME_INSTITUTION)));
    }

    @ParameterizedTest(name = "build throws exception when username is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void buildThrowsExceptionWhenUsernameIsNullOrEmpty(String username) {
        Executable action = () -> UserDto.newBuilder().withUsername(username).build();
        assertThrows(RuntimeException.class, action);
    }

    @Test
    void copyShouldCopyUserDto() {
        UserDto initialUser = createUserWithRolesAndInstitutionAndViewingScope();
        UserDto copiedUser = initialUser.copy().build();

        assertThat(copiedUser, is(equalTo(initialUser)));
        assertThat(copiedUser, is(not(sameInstance(initialUser))));
    }

    @Test
    void userDtoIsSerialized() throws IOException {
        UserDto initialUser = createUserWithRolesAndInstitutionAndViewingScope();

        assertThat(initialUser, doesNotHaveEmptyValues());

        String jsonString = JSON.std.asString(initialUser);

        var actualJson = JSON.std.mapFrom(jsonString);
        var expectedJson = toMap(initialUser);
        assertThat(actualJson, is(equalTo(expectedJson)));

        var deserializedObject = JSON.std.beanFrom(UserDto.class, jsonString);
        assertThat(deserializedObject, is(equalTo(initialUser)));
        assertThat(deserializedObject, is(not(sameInstance(initialUser))));
    }

    @Test
    void userDtoContainsCristinUnitsToBeIncludedToCuratorsView() throws BadRequestException {
        URI cristinUnitIncludedInDefaultCuratorsView = randomCristinOrgId();
        ViewingScope viewingScope = new ViewingScope(Set.of(cristinUnitIncludedInDefaultCuratorsView),
                                                     null);
        UserDto userDto = UserDto.newBuilder().withUsername(randomString())
            .withViewingScope(viewingScope)
            .build();
        ViewingScope actualViewingScope = userDto.getViewingScope();
        assertThat(actualViewingScope.getIncludedUnits(), contains(cristinUnitIncludedInDefaultCuratorsView));
    }

    @Test
    void userDtoContainsCristinUnitsToBeExcludedToCuratorsView() throws BadRequestException {
        URI cristinUnitIncludedInDefaultCuratorsView = randomCristinOrgId();
        URI cristinUnitExcludedFromDefaultCuratorsView = randomCristinOrgId();
        ViewingScope viewingScope = new ViewingScope(Set.of(cristinUnitIncludedInDefaultCuratorsView),
                                                     Set.of(cristinUnitExcludedFromDefaultCuratorsView));
        UserDto userDto = UserDto.newBuilder().withUsername(randomString())
            .withViewingScope(viewingScope)
            .build();
        ViewingScope actualViewingScope = userDto.getViewingScope();
        assertThat(actualViewingScope.getIncludedUnits(), contains(cristinUnitIncludedInDefaultCuratorsView));
        assertThat(actualViewingScope.getExcludedUnits(), contains(cristinUnitExcludedFromDefaultCuratorsView));
    }

    @Test
    void shouldSerializeAsJson() throws IOException {
        var sample = UserDto.newBuilder()
            .withUsername(randomString())
            .withFamilyName(randomString())
            .withGivenName(randomString())
            .withInstitution(randomUri())
            .withRoles(sampleRoles)
            .withViewingScope(randomViewingScope())
            .build();
        var json = sample.toString();
        var deserialized = JSON.std.beanFrom(UserDto.class,json);
        assertThat(deserialized,is(equalTo(sample)));
    }

    private static Set<RoleDto> createSampleRoles() {
        try {
            return Collections.singleton(createRole(SOME_ROLENAME));
        } catch (InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        }
    }

    private RoleDto sampleRole(String approveDoiRequest, String someRolename)
        throws InvalidEntryInternalException {
        Set<String> accessRights = Collections.singleton(approveDoiRequest);
        return RoleDto.newBuilder()
            .withRoleName(someRolename)
            .withAccessRights(accessRights)
            .build();
    }

    private List<RoleDto> duplicateRoles(UserDto user) {
        List<RoleDto> duplicateRoles = user.getRoles().stream()
            .map(attempt(r -> r.copy().withRoleName(r.getRoleName() + "_copy").build()))
            .flatMap(Try::stream)
            .collect(Collectors.toList());
        ArrayList<RoleDto> newRoles = new ArrayList<>(user.getRoles());
        newRoles.addAll(duplicateRoles);
        return newRoles;
    }
}
