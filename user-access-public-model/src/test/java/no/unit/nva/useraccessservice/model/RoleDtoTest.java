package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto.Builder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.hamcrest.core.IsSame;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.model.EntityUtils.SAMPLE_ACCESS_RIGHTS;
import static no.unit.nva.useraccessservice.model.EntityUtils.createRole;
import static no.unit.nva.useraccessservice.model.RoleDto.MISSING_ROLE_NAME_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleDtoTest extends DtoTest {

    protected static final String ROLE_TYPE_LITERAL = "Role";

    @Test
    void roleDtoShouldHaveABuilder() {
        Builder builder = RoleDto.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void builderShouldAllowSettingRoleName() throws InvalidEntryInternalException {
        RoleName roleName = randomRoleName();
        RoleDto role = RoleDto.newBuilder().withRoleName(roleName).build();
        assertThat(role.getRoleName(), is(equalTo(roleName)));
    }

    @Test
    void builderAllowsSettingAccessRights() throws InvalidEntryInternalException {
        RoleDto sampleRole = RoleDto.newBuilder()
                .withRoleName(randomRoleName())
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();

        assertThat(sampleRole.getAccessRights(), containsInAnyOrder(SAMPLE_ACCESS_RIGHTS.toArray(AccessRight[]::new)));
    }

    @Test
    void toStringReturnsStringContainingTheNameOfTheRole() {
        RoleDto role = RoleDto.newBuilder().withRoleName(randomRoleName()).build();
        assertThat(role.toString(), containsString(role.getRoleName().getValue()));
    }

    @Test
    void copyReturnsABuilderWithAllFieldsOfOriginalObjectPreserved()
            throws InvalidEntryInternalException {
        RoleDto original = RoleDto
                .newBuilder()
                .withRoleName(randomRoleName())
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();
        RoleDto copy = original.copy().build();
        assertThat(original, doesNotHaveEmptyValues());
        assertThat(copy, is(not(sameInstance(original))));
        assertThat(copy, is(equalTo(original)));
    }

    @ParameterizedTest(name = "isValid() returns false when username is null")
    @NullSource
    void isValidReturnsFalseWhenUsernameIsNullOrBlank(String NullRoleName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RoleDto roleDto = new RoleDto();
        Method setter = RoleDto.class.getDeclaredMethod("setRoleName", RoleName.class);
        setter.setAccessible(true);
        setter.invoke(roleDto, NullRoleName);
        assertThat(roleDto.isValid(), is(equalTo(false)));
    }

    @DisplayName("RoleDto object contains type with value \"Role\"")
    @Test
    void roleDtoSerializedObjectContainsTypeWithValueRole() throws InvalidEntryInternalException, IOException {
        RoleDto sampleRole = createRole(randomRoleName());
        var jsonMap = toMap(sampleRole);

        String actualType = jsonMap.get(JSON_TYPE_ATTRIBUTE).toString();
        assertThat(actualType, is(equalTo(ROLE_TYPE_LITERAL)));
    }

    @DisplayName("RoleDto cannot be created without type value")
    @Test
    @Disabled("We cannot do this when we are using Jackson-jr")
    void userDtoCannotBeCreatedWithoutTypeValue() throws InvalidEntryInternalException, IOException {
        RoleDto sampleUser = createRole(randomRoleName());
        var jsonMap = toMap(sampleUser);
        var objectWithoutType = jsonMap.remove(JSON_TYPE_ATTRIBUTE);
        String jsonStringWithoutType = JsonConfig.writeValueAsString(objectWithoutType);

        Executable action = () -> RoleDto.fromJson(jsonStringWithoutType);
        InvalidTypeIdException exception = assertThrows(InvalidTypeIdException.class, action);
        assertThat(exception.getMessage(), StringContains.containsString(RoleDto.TYPE));
    }

    @DisplayName("RoleDto can be created when it contains the right type value")
    @Test
    void roleDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
            throws InvalidEntryInternalException, IOException, InvalidInputException, BadRequestException {
        var someRole = createRole(randomRoleName());
        var jsonMap = JsonConfig.mapFrom(someRole.toString());
        assertThatSerializedItemContainsType(jsonMap, ROLE_TYPE_LITERAL);

        String jsonStringWithType = JsonConfig.writeValueAsString(jsonMap);
        RoleDto deserializedItem = RoleDto.fromJson(jsonStringWithType);

        assertThat(deserializedItem, is(equalTo(someRole)));
        assertThat(deserializedItem, is(not(IsSame.sameInstance(someRole))));
    }

    @Test
    void exceptionWhenInvalidReturnsInvalidInputException() throws InvalidEntryInternalException {
        RoleDto roleDto = createRole(randomRoleName());
        InvalidInputException exception = roleDto.exceptionWhenInvalid();

        assertThat(exception.getMessage(), StringContains.containsString(MISSING_ROLE_NAME_ERROR));
    }

    @Test
    void shouldSerializeAsJson() throws InvalidInputException, BadRequestException {
        var firstAccessRight = randomAccessRight();
        var secondAccessRight = randomAccessRight();
        while (secondAccessRight.equals(firstAccessRight)) {
            secondAccessRight = randomAccessRight();
        }
        var randomAccessRights = Set.of(firstAccessRight, secondAccessRight);
        var sample = RoleDto.newBuilder()
                .withRoleName(randomRoleName())
                .withAccessRights(randomAccessRights)
                .build();

        var json = sample.toString();
        var deserialized = RoleDto.fromJson(json);
        assertThat(deserialized, is(equalTo(sample)));
    }

    private AccessRight randomAccessRight() {
        return randomElement(AccessRight.values());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenInputCannotBeParsed() {
        var invalidInput = randomString();
        var exception = assertThrows(BadRequestException.class, () -> RoleDto.fromJson(invalidInput));
        assertThat(exception.getMessage(), containsString(invalidInput));
    }
}