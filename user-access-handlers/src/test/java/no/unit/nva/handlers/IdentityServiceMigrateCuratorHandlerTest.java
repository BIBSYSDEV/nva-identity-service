package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityServiceMigrateCuratorHandlerTest {

    private IdentityService identityService;
    private ByteArrayOutputStream output;
    private Context context;
    private LocalIdentityService identityServiceLocalDb;
    private IdentityServiceMigrateCuratorHandler handler;

    @BeforeEach
    public void init() {
        initializeIdentityService();
        handler = new IdentityServiceMigrateCuratorHandler(identityService);
        this.context = new FakeContext();

        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @AfterEach
    public void close() {
        this.identityServiceLocalDb.closeDB();
    }

    private void initializeIdentityService() {
        this.identityServiceLocalDb = new LocalIdentityService();
        this.identityServiceLocalDb.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(this.identityServiceLocalDb.getDynamoDbClient());
    }

    @Test
    void shouldRemoveAllDeprecatedRolesFromUser()
        throws NotFoundException, ConflictException, IOException, InvalidInputException {
        var deprecatedNviRole = RoleDto.newBuilder().withRoleName(RoleName.DEPRECATED_NVI_CURATOR).build();
        var deprecatedCuratorRole = RoleDto.newBuilder().withRoleName(RoleName.DEPRECATED_CURATOR).build();
        var roleToKeep = RoleDto.newBuilder().withRoleName(RoleName.CREATOR).build();
        identityService.addRole(deprecatedNviRole);
        identityService.addRole(deprecatedCuratorRole);
        identityService.addRole(roleToKeep);
        var user = createUserWithRoles(Set.of(deprecatedNviRole, deprecatedCuratorRole, roleToKeep));
        identityService.addUser(user);

        handler.handleRequest(createRequest(), output, context);

        var fetchedUser = this.identityService.getUser(user);

        assertTrue(fetchedUser.getRoles().stream().allMatch(RoleDto::isNotDeprecated));
        assertThat(fetchedUser.getRoles(), is(equalTo(Set.of(roleToKeep))));
    }

    @Test
    void shouldNotUpdateAlreadyMigratedUsers() throws InvalidInputException, ConflictException,
                                                      IOException, NotFoundException {
        var randomRole = RoleDto.newBuilder().withRoleName(RoleName.EDITOR).build();
        var newRole = newNviCuratorRole();
        identityService.addRole(randomRole);
        identityService.addRole(newRole);

        var user = createUserWithRoles(
            Set.of(randomRole, newRole)
        );
        identityService.addUser(user);

        handler.handleRequest(createRequest(), output, context);

        var fetchedUser = this.identityService.getUser(user);
        var roles = fetchedUser.getRoles().stream().map(RoleDto::getRoleName).toList();

        assertThat(roles, containsInAnyOrder(randomRole.getRoleName(), newRole.getRoleName()));
    }

    private RoleDto newNviCuratorRole() {
        return RoleDto.newBuilder().withRoleName(RoleName.NVI_CURATOR).build();
    }

    private InputStream createRequest() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }

    private UserDto createUserWithRoles(Set<RoleDto> roles) {
        return UserDto.newBuilder()
                   .withRoles(roles)
                   .withInstitution(randomUri())
                   .withUsername(randomString())
                   .withCristinId(randomUri())
                   .withFeideIdentifier(randomString())
                   .withInstitutionCristinId(randomCristinOrgId())
                   .withAffiliation(randomCristinOrgId())
                   .build();
    }


}