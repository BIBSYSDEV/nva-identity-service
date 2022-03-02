package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SAMPLE_ACCESS_RIGHTS;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SOME_ROLENAME;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.createRole;
import static no.unit.nva.useraccessmanagement.model.RoleDto.MISSING_ROLE_NAME_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto.Builder;
import org.hamcrest.core.IsSame;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RoleDtoTest extends DtoTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    protected static final String ROLE_TYPE_LITERAL = "Role";

    @Test
    void roleDtoShouldHaveABuilder() {
        Builder builder = RoleDto.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void builderShouldAllowSettingRoleName() throws InvalidEntryInternalException {
        RoleDto role = RoleDto.newBuilder().withRoleName(SOME_ROLE_NAME).build();
        assertThat(role.getRoleName(), is(equalTo(SOME_ROLE_NAME)));
    }

    @Test
    void builderAllowsSettingAccessRights() throws InvalidEntryInternalException {
        RoleDto sampleRole = RoleDto.newBuilder()
            .withRoleName(SOME_ROLE_NAME)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();

        assertThat(sampleRole.getAccessRights(), containsInAnyOrder(SAMPLE_ACCESS_RIGHTS.toArray(String[]::new)));
    }

    @ParameterizedTest(name = "builder should throw exception when rolename is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"", " "})
    void builderShouldNotAllowEmptyRoleName(String rolename) {
        Executable action = () -> RoleDto.newBuilder().withRoleName(rolename).build();
        assertThrows(InvalidEntryInternalException.class, action);
    }

    @Test
    void toStringReturnsStringContainingTheNameOfTheRole() {
        RoleDto role = RoleDto.newBuilder().withRoleName(SOME_ROLE_NAME).build();
        assertThat(role.toString(), containsString(role.getRoleName()));
    }

    @Test
    void copyReturnsABuilderWithAllFieldsOfOriginalObjectPreserved()
        throws InvalidEntryInternalException, InvalidInputException {
        RoleDto original = RoleDto
            .newBuilder()
            .withRoleName(SOME_ROLE_NAME)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();
        RoleDto copy = original.copy().build();
        assertThat(original, doesNotHaveNullOrEmptyFields());
        assertThat(copy, is(not(sameInstance(original))));
        assertThat(copy, is(equalTo(original)));
    }

    @ParameterizedTest(name = "isValid() returns false when username is \"{0}\"")
    @NullAndEmptySource
    void isValidReturnsFalseWhenUsernameIsNullOrBlank(String emptyOrNullRoleName)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RoleDto roleDto = new RoleDto();
        Method setter = RoleDto.class.getDeclaredMethod("setRoleName", String.class);
        setter.setAccessible(true);
        setter.invoke(roleDto, emptyOrNullRoleName);
        assertThat(roleDto.isValid(), is(equalTo(false)));
    }

    @DisplayName("RoleDto object contains type with value \"Role\"")
    @Test
    void roleDtoSerializedObjectContainsTypeWithValueRole() throws InvalidEntryInternalException, IOException {
        RoleDto sampleRole = createRole(SOME_ROLENAME);
        var jsonMap = toMap(sampleRole);

        String actualType = jsonMap.get(JSON_TYPE_ATTRIBUTE).toString();
        assertThat(actualType, is(equalTo(ROLE_TYPE_LITERAL)));
    }

    @DisplayName("RoleDto cannot be created without type value")
    @Test
    @Disabled("We cannot do this when we are using Jackson-jr")
    void userDtoCannotBeCreatedWithoutTypeValue() throws InvalidEntryInternalException, IOException {
        RoleDto sampleUser = createRole(SOME_ROLE_NAME);
        var jsonMap = toMap(sampleUser);
        var objectWithoutType = jsonMap.remove(JSON_TYPE_ATTRIBUTE);
        String jsonStringWithoutType = JSON.std.asString(objectWithoutType);

        Executable action = () -> JSON.std.beanFrom(RoleDto.class, jsonStringWithoutType);
        InvalidTypeIdException exception = assertThrows(InvalidTypeIdException.class, action);
        assertThat(exception.getMessage(), StringContains.containsString(RoleDto.TYPE));
    }

    @DisplayName("RoleDto can be created when it contains the right type value")
    @Test
    void roleDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
        throws InvalidEntryInternalException, IOException {
        var someRole = createRole(SOME_ROLE_NAME);
        var jsonMap = toMap(someRole);
        assertThatSerializedItemContainsType(jsonMap, ROLE_TYPE_LITERAL);

        String jsonStringWithType = JSON.std.asString(jsonMap);
        RoleDto deserializedItem = JSON.std.beanFrom(RoleDto.class, jsonStringWithType);

        assertThat(deserializedItem, is(equalTo(someRole)));
        assertThat(deserializedItem, is(not(IsSame.sameInstance(someRole))));
    }

    @Test
    void exceptionWhenInvalidReturnsInvalidInputException() throws InvalidEntryInternalException {
        RoleDto roleDto = createRole(SOME_ROLE_NAME);
        InvalidInputException exception = roleDto.exceptionWhenInvalid();

        assertThat(exception.getMessage(), StringContains.containsString(MISSING_ROLE_NAME_ERROR));
    }
}