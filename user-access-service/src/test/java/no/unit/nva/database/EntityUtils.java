package no.unit.nva.database;

import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;

import java.util.Arrays;
import java.util.stream.Collectors;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

public final class EntityUtils {

    public static final String SOME_ROLENAME = "SomeRole";

    public static RoleDto createRole(RoleName roleName) throws InvalidEntryInternalException {
        return createRole(roleName, AccessRight.MANAGE_DOI);
    }

    public static RoleDto createRole(RoleName roleName, AccessRight... accessRights) {
        var accessRightSet = Arrays.stream(accessRights).collect(Collectors.toSet());

        return RoleDto.newBuilder()
                .withRoleName(roleName)
                .withAccessRights(accessRightSet)
                .build();
    }

    public static UserDto createUser() {
        return UserDto.newBuilder().withUsername(randomString()).build();
    }

    public static RoleName randomRoleName() {
        return RoleName.values()[randomInteger(RoleName.values().length)];
    }
}
