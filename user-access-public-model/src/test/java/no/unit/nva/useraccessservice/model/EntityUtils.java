package no.unit.nva.useraccessservice.model;

import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.apigateway.AccessRight;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static org.hamcrest.MatcherAssert.assertThat;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
            Collections.singleton(MANAGE_DOI);
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    /**
     * Intention is to create a user with all fields filled.
     *
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRolesAndInstitutionAndViewingScope()
            throws InvalidEntryInternalException {
        var user = createUserWithRoleWithoutInstitution(randomRoleName()).copy()
                .withInstitution(SOME_INSTITUTION)
                .withViewingScope(randomViewingScope())
                .withCristinId(randomUri())
                .withFeideIdentifier(randomString())
                .withInstitutionCristinId(randomCristinOrgId())
                .withAffiliation(randomCristinOrgId())
                .build();
        assertThat(user, doesNotHaveEmptyValues());
        return user;
    }

    /**
     * Creates a user with username and a role but without institution.
     *
     * @return {@link UserDto}
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRoleWithoutInstitution(RoleName roleName)
            throws InvalidEntryInternalException {
        RoleDto sampleRole = createRole(roleName);
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
     * @param roleName The sample role role name.
     * @return the sample role
     * @throws InvalidEntryInternalException when the generated role is invalid.
     */
    public static RoleDto createRole(RoleName roleName) throws InvalidEntryInternalException {
        return
                RoleDto.newBuilder()
                        .withRoleName(roleName)
                        .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                        .build();
    }
}
