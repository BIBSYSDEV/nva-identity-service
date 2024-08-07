package no.unit.nva.handlers;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.handlers.data.DefaultRoleSource;
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
    void shouldAddManageResourceFilesRoleToPublishingCurator()
        throws NotFoundException, ConflictException, IOException, InvalidInputException {
        var roleToUpdate = RoleDto.newBuilder().withRoleName(RoleName.PUBLISHING_CURATOR)
                          .withAccessRights(Collections.emptySet()).build();
        var roleToKeep = RoleDto.newBuilder().withRoleName(RoleName.SUPPORT_CURATOR)
                          .withAccessRights(Collections.emptySet()).build();
        identityService.addRole(roleToUpdate);
        identityService.addRole(roleToKeep);
        var user = createUserWithRoles(Set.of(roleToUpdate, roleToKeep));
        identityService.addUser(user);

        handler.handleRequest(createRequest(), output, context);

        var fetchedUser = this.identityService.getUser(user);

        assertThat(fetchedUser.getRoles(), hasItem(DefaultRoleSource.PUBLISHING_CURATOR_ROLE));
        assertThat(fetchedUser.getRoles(), hasItem(roleToKeep));
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