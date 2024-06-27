package no.unit.nva.useraccessservice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RoleNameTest {

    @ParameterizedTest
    @EnumSource(RoleName.class)
    void shouldConvertStringToRoleNameEnum(RoleName roleName) {
        assertEquals(roleName, RoleName.fromValue(roleName.getValue()));
    }
}