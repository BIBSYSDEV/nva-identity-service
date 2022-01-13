package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.useraccessmanagement.RestConfig.defaultRestObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.RandomUserDataGenerator;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import nva.commons.apigateway.exceptions.BadRequestException;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final String SOME_ROLENAME = "SomeRole";
    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final String EMPTY_STRING = "";
    public static final Set<String> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton("APPROVE_DOI_REQUEST");
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    /**
     * Creates a request for adding a user without a username. To be used with {@code handleRequest()} method.
     *
     * @return an RequestBuilder that can produce an {@link InputStream} that contains a request to be processed by a
     *     {@link com.amazonaws.services.lambda.runtime.RequestStreamHandler}.
     * @throws JsonProcessingException       if JSON serialization fails.
     * @throws InvalidEntryInternalException unlikely. The object is intentionally invalid.
     * @throws InvalidEntryInternalException when role is invalid.
     * @throws NoSuchMethodException         reflection related.
     * @throws IllegalAccessException        reflection related.
     * @throws InvocationTargetException     reflection related.
     */
    public static HandlerRequestBuilder<UserDto> createRequestBuilderWithUserWithoutUsername()
        throws JsonProcessingException, InvalidEntryInternalException,
               NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidInputException,
               BadRequestException {
        UserDto userWithoutUsername = createUserWithoutUsername();
        return new HandlerRequestBuilder<UserDto>(defaultRestObjectMapper)
            .withBody(userWithoutUsername);
    }


    /**
     * Creates a user without a username. For testing output on invalid input.
     *
     * @return a {@link UserDto}
     * @throws InvalidEntryInternalException when the added role is invalid.
     * @throws InvalidEntryInternalException unlikely.  The object is intentionally invalid.
     * @throws NoSuchMethodException         reflection related.
     * @throws InvocationTargetException     reflection related.
     * @throws IllegalAccessException        reflection related.
     */
    public static UserDto createUserWithoutUsername()
        throws InvalidEntryInternalException, NoSuchMethodException,
               InvocationTargetException, IllegalAccessException, InvalidInputException, BadRequestException {
        UserDto userWithoutUsername = createUserWithRolesAndInstitutionAndViewingScope();
        Method method = userWithoutUsername.getClass().getDeclaredMethod("setUsername", String.class);
        method.setAccessible(true);
        method.invoke(userWithoutUsername, EMPTY_STRING);

        return userWithoutUsername;
    }

    /**
     * create user without roles.
     *
     * @return {@link UserDto}
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user
     */
    public static UserDto createUserWithoutRoles() throws InvalidEntryInternalException {
        return UserDto.newBuilder().withUsername(SOME_USERNAME).build();
    }

    /**
     * Intention is to create a user with all fields filled.
     *
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRolesAndInstitutionAndViewingScope()
        throws InvalidEntryInternalException, InvalidInputException, BadRequestException {
        return createUserWithRoleWithoutInstitution().copy()
            .withInstitution(SOME_INSTITUTION)
            .withViewingScope(randomViewingScope())
            .build();
    }


    /**
     * Creates a a user with username and a role but without institution.
     *
     * @return {@link UserDto}
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRoleWithoutInstitution()
        throws InvalidEntryInternalException {
        RoleDto sampleRole = createRole(SOME_ROLENAME);
        return UserDto.newBuilder()
            .withUsername(SOME_USERNAME)
            .withGivenName(SOME_GIVEN_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withRoles(Collections.singletonList(sampleRole))
            .build();
    }

    /**
     * Creates a sample role.
     *
     * @param someRole The sample role role name.
     * @return the sample role
     * @throws InvalidEntryInternalException when the generated role is invalid.
     */
    public static RoleDto createRole(String someRole) throws InvalidEntryInternalException {
        return
            RoleDto.newBuilder()
                .withName(someRole)
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();
    }
}
