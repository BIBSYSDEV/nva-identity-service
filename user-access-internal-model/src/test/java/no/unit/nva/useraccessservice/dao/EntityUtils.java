package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.useraccessservice.accessrights.AccessRight.APPROVE_DOI_REQUEST;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final String SOME_ROLENAME = "SomeRole";
    public static final URI SOME_INSTITUTION = randomCristinOrgId();

    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton(APPROVE_DOI_REQUEST);
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";

    /**
     * Intention is to create a user with all fields filled.
     */
    public static UserDto createUserWithRolesAndInstitution() throws InvalidInputException {
        return createUserWithRoleWithoutInstitution().copy()
            .withInstitution(SOME_INSTITUTION)
            .build();
    }

    /**
     * Creates a a user with username and a role but without institution.
     *
     * @return {@link UserDto}
     */
    public static UserDto createUserWithRoleWithoutInstitution() throws InvalidInputException {
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
     * @param someRole the role name.
     * @return the sample role.
     */
    public static RoleDto createRole(String someRole) throws InvalidInputException {
        Set<String> accessRights = SAMPLE_ACCESS_RIGHTS
            .stream()
            .map(AccessRight::toString)
            .collect(Collectors.toSet());
        return
            RoleDto.newBuilder()
                .withRoleName(someRole)
                .withAccessRights(accessRights)
                .build();
    }
}
