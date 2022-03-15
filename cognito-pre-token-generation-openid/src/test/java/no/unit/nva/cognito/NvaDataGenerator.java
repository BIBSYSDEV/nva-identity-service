package no.unit.nva.cognito;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.Collection;
import java.util.List;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;

public final class NvaDataGenerator {

    private NvaDataGenerator() {

    }

    public static UserDto createRandomUser(String username) {
        return UserDto.newBuilder()
            .withUsername(username)
            .withCristinId(randomUri().toString())
            .withRoles(randomRoles())
            .withGivenName(randomString())
            .withFamilyName(randomString())
            .withInstitution(randomUri())
            .build();
    }

    private static Collection<RoleDto> randomRoles() {
        return List.of(randomRole(), randomRole());
    }

    private static RoleDto randomRole() {
        return RoleDto.newBuilder()
            .withRoleName(randomString())
            .build();
    }
}
