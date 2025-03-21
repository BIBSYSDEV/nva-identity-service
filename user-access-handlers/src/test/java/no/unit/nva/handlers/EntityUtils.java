package no.unit.nva.handlers;

import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.core.attempt.Try.attempt;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";

    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final String EMPTY_STRING = "";
    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton(MANAGE_DOI);
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

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
        RoleDto sampleRole = createRole(RoleName.CREATOR);
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
     * @param roleName the role.
     * @return the role.
     * @throws InvalidEntryInternalException when generated role is invalid.
     */
    public static RoleDto createRole(RoleName roleName) throws InvalidEntryInternalException {
        return
            RoleDto.newBuilder()
                .withRoleName(roleName)
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();
    }
}
