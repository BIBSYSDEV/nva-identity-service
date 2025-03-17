package no.unit.nva.useraccessservice.dao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleNameTest {

    @ParameterizedTest
    @EnumSource(RoleName.class)
    void shouldConvertStringToRoleNameEnum(RoleName roleName) {
        assertEquals(roleName, RoleName.fromValue(roleName.getValue()));
    }
}