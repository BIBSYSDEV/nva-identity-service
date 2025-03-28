package no.unit.nva.useraccessservice.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.EntityUtils.SOME_USERNAME;
import static no.unit.nva.useraccessservice.model.EntityUtils.createRole;
import static no.unit.nva.useraccessservice.model.EntityUtils.createUserWithRoleWithoutInstitution;
import static no.unit.nva.useraccessservice.model.EntityUtils.createUserWithRolesAndInstitutionAndViewingScope;
import static no.unit.nva.useraccessservice.model.UserDto.AT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserDtoTest extends DtoTest {

    public static final Set<RoleDto> sampleRoles = createSampleRoles();

    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final int ROLE_PART = 0;
    protected static final String USER_TYPE_LITERAL = "User";
    private static final AccessRight FIRST_ACCESS_RIGHT = AccessRight.MANAGE_DOI;
    private static final AccessRight SECOND_ACCESS_RIGHT = AccessRight.MANAGE_CUSTOMERS;

    private static Set<RoleDto> createSampleRoles() {
        try {
            return Collections.singleton(createRole(randomRoleName()));
        } catch (InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        }
    }

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
        String jsonStringWithoutType = JsonConfig.writeValueAsString(jsonMap);
        Executable action = () -> JsonConfig.readValue(jsonStringWithoutType, UserDto.class);
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

        String jsonStringWithType = JsonConfig.writeValueAsString(json);
        UserDto deserializedItem = JsonConfig.readValue(jsonStringWithType, UserDto.class);

        assertThat(deserializedItem, is(equalTo(sampleUser)));
        assertThat(deserializedItem, is(not(sameInstance(sampleUser))));
    }

    @Test
    void userDtoShouldContainEmptyViewingScopeWhenViewingScopeIsNotProvided()
        throws InvalidEntryInternalException, IOException {
        UserDto sampleUser = UserDto.newBuilder().build();
        var json = toMap(sampleUser);

        var viewingScope = toMap(JsonUtils.dtoObjectMapper.writeValueAsString(json.get("viewingScope")));
        assertThat(viewingScope.get("includedUnits"), is(notNullValue()));
        assertThat(viewingScope.get("excludedUnits"), is(notNullValue()));
    }

    @Test
    void getAccessRightsReturnsAccessRightsWithoutDuplicates() {
        final UserDto user = createUserWithRoleWithoutInstitution(sampleRoles.iterator().next().getRoleName());
        final var expectedAccessRights = new HashSet<>(user.getAccessRights());
        List<RoleDto> newRoles = duplicateRoles(user);
        UserDto newUser = user.copy().withRoles(newRoles).build();

        var actualAccessRights = new HashSet<>(newUser.getAccessRights());
        assertThat(actualAccessRights, is(equalTo(expectedAccessRights)));
    }

    private List<RoleDto> duplicateRoles(UserDto user) {
        List<RoleDto> duplicateRoles = user.getRoles().stream()
            .map(attempt(r -> r.copy().withRoleName(r.getRoleName()).build()))
            .flatMap(Try::stream)
            .toList();
        ArrayList<RoleDto> newRoles = new ArrayList<>(user.getRoles());
        newRoles.addAll(duplicateRoles);
        return newRoles;
    }

    @Test
    void getAccessRightsReturnsAllAccessRightsContainedInTheUsersRoles() {

        RoleName someRolename = randomRoleName();
        RoleDto firstRole = sampleRole(FIRST_ACCESS_RIGHT, someRolename);
        RoleDto secondRole = sampleRole(SECOND_ACCESS_RIGHT, randomRoleNameButNot(someRolename));

        List<RoleDto> roles = List.of(firstRole, secondRole);
        UserDto user = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(roles).build();

        var expectedAccessRights = Set.of(FIRST_ACCESS_RIGHT, SECOND_ACCESS_RIGHT);
        assertThat(user.getAccessRights(), containsInAnyOrder(expectedAccessRights.toArray(AccessRight[]::new)));
    }

    private RoleDto sampleRole(AccessRight accessRight, RoleName someRolename)
        throws InvalidEntryInternalException {
        var accessRights = Collections.singleton(accessRight);
        return RoleDto.newBuilder()
            .withRoleName(someRolename)
            .withAccessRights(accessRights)
            .build();
    }

    @Test
    void getRolesReturnsEmptyListWhenRolesIsNull() {
        UserDto userDto = UserDto.newBuilder().withUsername(SOME_USERNAME).withRoles(null).build();
        var roles = userDto.getRoles();
        assertThat(roles, is(not(nullValue())));
        assertThat(roles, is(empty()));
    }

    @Test
    void builderReturnsUserDtoWhenInstitutionIsEmpty() {
        UserDto user = createUserWithRoleWithoutInstitution(sampleRoles.iterator().next().getRoleName());
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

    @ParameterizedTest
    @MethodSource("invalidNameProvider")
    void buildThrowsExceptionWhenUsernameIsNullOrEmpty(String username) {
        Executable action = () -> UserDto.newBuilder().withUsername(username).build();
        assertThrows(RuntimeException.class, action);
    }

    private static Stream<Arguments> invalidNameProvider() {
        return Stream.of(argumentSet("null string", (String) null),
                         argumentSet("Empty string", ""),
                         argumentSet("Space character", " "),
                         argumentSet("Tab character", "\t"),
                         argumentSet("Newline character", "\n"));
    }

    @Test
    void shouldCopyWithoutInformationLoss() {
        UserDto initialUser = createUserWithRolesAndInstitutionAndViewingScope();
        UserDto copiedUser = initialUser.copy().build();

        assertThat(copiedUser, is(equalTo(initialUser)));
        assertThat(copiedUser, is(not(sameInstance(initialUser))));
    }

    @Test
    void userDtoContainsCristinUnitsToBeIncludedToCuratorsView() throws BadRequestException {
        URI cristinUnitIncludedInDefaultCuratorsView = randomCristinOrgId();
        ViewingScope viewingScope = ViewingScope.create(Set.of(cristinUnitIncludedInDefaultCuratorsView), null);
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
        ViewingScope viewingScope = ViewingScope.create(Set.of(cristinUnitIncludedInDefaultCuratorsView),
            Set.of(cristinUnitExcludedFromDefaultCuratorsView));
        UserDto userDto = UserDto.newBuilder().withUsername(randomString())
            .withViewingScope(viewingScope)
            .build();
        ViewingScope actualViewingScope = userDto.getViewingScope();
        assertThat(actualViewingScope.getIncludedUnits(), contains(cristinUnitIncludedInDefaultCuratorsView));
        assertThat(actualViewingScope.getExcludedUnits(), contains(cristinUnitExcludedFromDefaultCuratorsView));
    }

    @Test
    void shouldSerializeAsJson() throws BadRequestException {
        var sample = UserDto.newBuilder()
            .withUsername(randomString())
            .withFamilyName(randomString())
            .withGivenName(randomString())
            .withInstitution(randomUri())
            .withRoles(sampleRoles)
            .withViewingScope(randomViewingScope())
            .build();
        var json = sample.toString();
        var deserialized = UserDto.fromJson(json);
        assertThat(deserialized, is(equalTo(sample)));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenFailingToDeserialize() {
        var invalidJson = randomString();
        var exception = assertThrows(BadRequestException.class, () -> UserDto.fromJson(invalidJson));
        assertThat(exception.getMessage(), containsString(invalidJson));
    }

    @Test
    void shouldGenerateRoleClaimStringsForCognito() {
        var user = createUserWithRolesAndInstitutionAndViewingScope();
        var roleClaims = user.generateRoleClaims().collect(Collectors.toSet());
        var expectedRolenames = user.getRoles().stream()
            .map(RoleDto::getRoleName)
            .map(RoleName::getValue)
            .collect(Collectors.toSet());
        assertThatAllClaimContainTheInstitutionId(user, roleClaims);
        assertThatThereIsARoleClaimForEachRole(user, expectedRolenames);
    }

    private void assertThatThereIsARoleClaimForEachRole(UserDto user, Set<String> expectedRolenames) {
        var actualRolenamesInClaims = user.generateRoleClaims()
            .map(claim -> claim.split(AT)[ROLE_PART])
            .collect(Collectors.toSet());
        assertThat(actualRolenamesInClaims, is(equalTo(expectedRolenames)));
    }

    private void assertThatAllClaimContainTheInstitutionId(UserDto user, Set<String> roleClaims) {
        for (var roleClaim : roleClaims) {
            assertThat(roleClaim, containsString(user.getInstitution().toString()));
        }
    }

    @Test
    void shouldReturnViewingScopeSetToTopLevelOrgWhenScopeIsNotExplicitlyDefinedAndUserIsConnectedToSomeTopLevelOrg() {
        var user = createUserWithRoleAndInstitutionWithoutViewingScope();
        var viewingScope = user.getViewingScope();
        var expectedViewingScope = ViewingScope.defaultViewingScope(user.getInstitutionCristinId());
        assertThat(viewingScope, is(equalTo(expectedViewingScope)));
    }

    private UserDto createUserWithRoleAndInstitutionWithoutViewingScope() {
        var user = createUserWithRoleWithoutInstitution(sampleRoles.iterator().next().getRoleName())
            .copy()
            .withInstitutionCristinId(randomCristinOrgId())
            .withInstitution(randomUri())
            .withAffiliation(randomCristinOrgId())
            .withViewingScope(null)
            .withFeideIdentifier(randomString())
            .withCristinId(randomUri())
            .build();
        assertThat(user, doesNotHaveEmptyValuesIgnoringFields(Set.of("viewingScope")));
        return user;
    }

    @Test
    void shouldReturnViewingScopeWithEmptySetsWhenScopeIsNotExplicitlyDefinedAndUserIsNotConnectedToSomeTopLevelOrg() {
        var user = createUserWithRoleWithoutInstitution(sampleRoles.iterator().next().getRoleName());
        var viewingScope = user.getViewingScope();
        assertThat(viewingScope, is(notNullValue()));
    }
}
