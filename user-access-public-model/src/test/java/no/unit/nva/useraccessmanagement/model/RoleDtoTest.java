package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SAMPLE_ACCESS_RIGHTS;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.SOME_ROLENAME;
import static no.unit.nva.useraccessmanagement.model.EntityUtils.createRole;
import static no.unit.nva.useraccessmanagement.model.RoleDto.MISSING_ROLE_NAME_ERROR;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto.Builder;
import org.hamcrest.core.IsSame;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class RoleDtoTest extends DtoTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    protected static final String ROLE_TYPE_LITERAL = "Role";

    @Test
    public void roleDtoShouldHaveABuilder() {
        Builder builder = RoleDto.newBuilder();
        assertNotNull(builder);
    }

    @Test
    public void builderShouldAllowSettingRoleName() throws InvalidEntryInternalException {
        RoleDto role = RoleDto.newBuilder().withName(SOME_ROLE_NAME).build();
        assertThat(role.getRoleName(), is(equalTo(SOME_ROLE_NAME)));
    }

    @Test
    public void builderAllowsSettingAccessRights() throws InvalidEntryInternalException {
        RoleDto sampleRole = RoleDto.newBuilder()
            .withName(SOME_ROLE_NAME)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();

        assertThat(sampleRole.getAccessRights(), is(equalTo(SAMPLE_ACCESS_RIGHTS)));
    }

    @ParameterizedTest(name = "builder should throw exception when rolename is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"", " "})
    public void builderShouldNotAllowEmptyRoleName(String rolename) {
        Executable action = () -> RoleDto.newBuilder().withName(rolename).build();
        assertThrows(InvalidEntryInternalException.class, action);
    }

    @Test
    public void toStringReturnsStringContainingTheNameOfTheRole() throws InvalidEntryInternalException {
        RoleDto role = RoleDto.newBuilder().withName(SOME_ROLE_NAME).build();
        assertThat(role.toString(), containsString(role.getRoleName()));
    }

    @Test
    public void copyReturnsABuilderWithAllFieldsOfOriginalObjectPreserved() throws InvalidEntryInternalException {
        RoleDto original = RoleDto
            .newBuilder()
            .withName(SOME_ROLE_NAME)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();
        RoleDto copy = original.copy().build();
        assertThat(original, doesNotHaveNullOrEmptyFields());
        assertThat(copy, is(not(sameInstance(original))));
        assertThat(copy, is(equalTo(original)));
    }

    @ParameterizedTest(name = "isValid() returns false when username is \"{0}\"")
    @NullAndEmptySource
    public void isValidReturnsFalseWhenUsernameIsNullOrBlank(String emptyOrNullRoleName)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RoleDto roleDto = new RoleDto();
        Method setter = RoleDto.class.getDeclaredMethod("setRoleName", String.class);
        setter.setAccessible(true);
        setter.invoke(roleDto, emptyOrNullRoleName);
        assertThat(roleDto.isValid(), is(equalTo(false)));
    }

    @DisplayName("RoleDto object contains type with value \"Role\"")
    @Test
    public void roleDtoSerializedObjectContainsTypeWithValueRole() throws InvalidEntryInternalException {
        RoleDto sampleRole = createRole(SOME_ROLENAME);
        ObjectNode json = objectMapper.convertValue(sampleRole, ObjectNode.class);

        String actualType = json.get(JSON_TYPE_ATTRIBUTE).asText();
        assertThat(actualType, is(equalTo(ROLE_TYPE_LITERAL)));
    }

    @DisplayName("RoleDto cannot be created without type value")
    @Test
    public void userDtoCannotBeCreatedWithoutTypeValue() throws InvalidEntryInternalException, JsonProcessingException {
        RoleDto sampleUser = createRole(SOME_ROLE_NAME);
        ObjectNode json = objectMapper.convertValue(sampleUser, ObjectNode.class);
        JsonNode objectWithoutType = json.remove(JSON_TYPE_ATTRIBUTE);
        String jsonStringWithoutType = objectMapper.writeValueAsString(objectWithoutType);

        Executable action = () -> objectMapper.readValue(jsonStringWithoutType, RoleDto.class);
        InvalidTypeIdException exception = assertThrows(InvalidTypeIdException.class, action);
        assertThat(exception.getMessage(), StringContains.containsString(INVALID_TYPE_EXCEPTION_MESSAGE_SAMPLE));
    }

    @DisplayName("RoleDto can be created when it contains the right type value")
    @Test
    public void roleDtoCanBeDeserializedWhenItContainsTheRightTypeValue()
        throws InvalidEntryInternalException, IOException {
        RoleDto someRole = createRole(SOME_ROLE_NAME);
        ObjectNode json = objectMapper.convertValue(someRole, ObjectNode.class);
        assertThatSerializedItemContainsType(json, ROLE_TYPE_LITERAL);

        String jsonStringWithType = objectMapper.writeValueAsString(json);

        RoleDto deserializedItem = objectMapper.readValue(jsonStringWithType, RoleDto.class);

        assertThat(deserializedItem, is(equalTo(someRole)));
        assertThat(deserializedItem, is(not(IsSame.sameInstance(someRole))));
    }

    @Test
    public void exceptionWhenInvalidReturnsInvalidInputException() throws InvalidEntryInternalException {
        RoleDto roleDto = createRole(SOME_ROLE_NAME);
        InvalidInputException exception = roleDto.exceptionWhenInvalid();

        assertThat(exception.getMessage(), StringContains.containsString(MISSING_ROLE_NAME_ERROR));
    }
}