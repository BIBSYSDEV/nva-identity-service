package no.unit.nva.useraccessservice.dao;

import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final URI SOME_INSTITUTION = randomCristinOrgId();

    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton(MANAGE_DOI);
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    /**
     * Intention is to create a user with all fields filled.
     */
    public static UserDto createUserWithRolesAndInstitution() {
        return createUserWithRoleWithoutInstitution().copy()
            .withInstitution(SOME_INSTITUTION)
            .build();
    }

    /**
     * Creates a a user with username and a role but without institution.
     *
     * @return {@link UserDto}
     */
    public static UserDto createUserWithRoleWithoutInstitution() {
        RoleDto sampleRole = createRole(randomRoleName());
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
     * @param roleName the role name.
     * @return the sample role.
     */
    public static RoleDto createRole(RoleName roleName) {
        return RoleDto.newBuilder()
            .withRoleName(roleName)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();
    }
}
