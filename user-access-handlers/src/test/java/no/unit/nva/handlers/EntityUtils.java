package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import no.unit.nva.identityservice.json.JsonConfig;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;

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
     */
    public static APIGatewayProxyRequestEvent createRequestBuilderWithUserWithoutUsername() {

        var userWithoutUsername = createUserWithoutUsername();
        var jsonString = attempt(() -> JsonConfig.asString(userWithoutUsername)).orElseThrow();
        return new APIGatewayProxyRequestEvent().withBody(jsonString);
    }

    /**
     * Creates a request for adding a user without a username. To be used with {@code handleRequest()} method.
     *
     * @return an InputStream containing the ApiGateway request to be handled by a {@link
     *     com.amazonaws.services.lambda.runtime.RequestStreamHandler}.
     */
    public static APIGatewayProxyRequestEvent createRequestWithUserWithoutUsername() {
        return createRequestBuilderWithUserWithoutUsername();
    }

    /**
     * Creates a user without a username. For testing output on invalid input.
     *
     * @return a {@link UserDto}
     */
    public static Map<String, Object> createUserWithoutUsername()
        throws InvalidEntryInternalException {
        UserDto userDto = createUserWithRolesAndInstitution();
        var jsonMap = attempt(userDto::toString).map(JsonConfig::mapFrom).orElseThrow();
        jsonMap.put(UserDto.USERNAME_FIELD, EMPTY_STRING);
        return jsonMap;
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
    public static UserDto createUserWithRolesAndInstitution()
        throws InvalidEntryInternalException {
        return createUserWithRoleWithoutInstitution().copy()
            .withInstitution(SOME_INSTITUTION)
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
     * @param someRole the role.
     * @return the role.
     * @throws InvalidEntryInternalException when generated role is invalid.
     */
    public static RoleDto createRole(String someRole) throws InvalidEntryInternalException {
        return
            RoleDto.newBuilder()
                .withRoleName(someRole)
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();
    }
}
