package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
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
    void shouldUpdateUserWhenItHasLegacyCuratorRole()
        throws NotFoundException, ConflictException, IOException, InvalidInputException {
        var oldRole = legacyCuratorRole();
        var newRole = newDoiCuratorRole();
        identityService.addRole(oldRole);
        identityService.addRole(newRole);

        var user = createUserWithRoles(
                           Set.of(oldRole)
                       );
        identityService.addUser(user);

        handler.handleRequest(createRequest(), output, context);

        var fetchedUser = this.identityService.getUser(user);
        var roleNames = fetchedUser.getRoles().stream().map(RoleDto::getRoleName).toList();

        assertThat(roleNames, contains(newRole.getRoleName()));
        assertThat(roleNames, not(contains(oldRole.getRoleName())));
    }

    @Test
    void shouldNotUpdateAlreadyMigratedUsers() throws InvalidInputException, ConflictException,
                                                      IOException, NotFoundException {
        var randomRole = RoleDto.newBuilder().withRoleName(randomString()).build();
        var newRole = newDoiCuratorRole();
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

    private RoleDto legacyCuratorRole() {
        return RoleDto.newBuilder().withRoleName("Curator").build();
    }

    private RoleDto newDoiCuratorRole() {
        return RoleDto.newBuilder().withRoleName("Doi-Curator").build();
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