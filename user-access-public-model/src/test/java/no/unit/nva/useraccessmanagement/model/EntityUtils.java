package no.unit.nva.useraccessmanagement.model;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.RandomUserDataGenerator.randomViewingScope;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final String SOME_ROLENAME = "SomeRole";
    public static final URI SOME_INSTITUTION = randomCristinOrgId();
    public static final Set<String> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton("APPROVE_DOI_REQUEST");
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";


    /**
     * Intention is to create a user with all fields filled.
     *
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRolesAndInstitutionAndViewingScope()
        throws InvalidEntryInternalException {
        var user= createUserWithRoleWithoutInstitution().copy()
            .withInstitution(SOME_INSTITUTION)
            .withViewingScope(randomViewingScope())
            .withCristinId(randomUri())
            .build();
        assertThat(user, doesNotHaveEmptyValues());
        return user;
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
                .withRoleName(someRole)
                .withAccessRights(SAMPLE_ACCESS_RIGHTS)
                .build();
    }
}
