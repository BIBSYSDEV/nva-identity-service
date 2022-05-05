package no.unit.nva.database;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;

public final class EntityUtils {

    public static final String SOME_USERNAME = "SomeUsername";
    public static final String SOME_ROLENAME = "SomeRole";
    public static final String SOME_INSTITUTION = "SomeInstitution";
    public static final String EMPTY_STRING = "";
    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton(AccessRight.APPROVE_DOI_REQUEST);
    private static final String SOME_GIVEN_NAME = "givenName";
    private static final String SOME_FAMILY_NAME = "familyName";



    /**
     * Creates a a user with username and a role but without institution.
     *
     * @return {@link UserDto}
     * @throws InvalidEntryInternalException When the user is invalid. The user is supposed to be a valid user.
     */
    public static UserDto createUserWithRoleWithoutInstitution()
        throws InvalidEntryInternalException, InvalidInputException {
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
        Set<String> sampleAccessRights = SAMPLE_ACCESS_RIGHTS
            .stream()
            .map(AccessRight::toString)
            .collect(Collectors.toSet());
        return
            RoleDto.newBuilder()
                .withRoleName(someRole)
                .withAccessRights(sampleAccessRights)
                .build();
    }
}
