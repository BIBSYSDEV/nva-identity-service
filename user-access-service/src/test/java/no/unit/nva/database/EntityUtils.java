package no.unit.nva.database;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.AccessRight;

public final class EntityUtils {

    public static final String SOME_ROLENAME = "SomeRole";
    public static final Set<AccessRight> SAMPLE_ACCESS_RIGHTS =
        Collections.singleton(AccessRight.APPROVE_DOI_REQUEST);

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
