package no.unit.nva.handlers;

import static no.unit.nva.handlers.IdentityServiceInitHandler.SIKT_ACTING_USER;
import static no.unit.nva.handlers.IdentityServiceInitHandler.SIKT_CRISTIN_ID;
import static no.unit.nva.useraccessservice.model.RoleDto.MISSING_ROLE_NAME_ERROR;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityServiceInitHandlerTest {

    private static final List<AccessRight> ACCESS_RIGHTS = List.of(MANAGE_DOI,
                                                                   MANAGE_PUBLISHING_REQUESTS);
    private static final RoleSource ROLE_SOURCE = () -> List.of(RoleDto.newBuilder()
                                                                    .withRoleName(RoleName.DOI_CURATOR)
                                                                    .withAccessRights(ACCESS_RIGHTS)
                                                                    .build());
    public static final String BACKEND_CLIENT_ID = "some-client-id";

    private IdentityService identityService;
    private ByteArrayOutputStream output;
    private Context context;
    private LocalCustomerServiceDatabase customerServiceLocalDb;
    private LocalIdentityService identityServiceLocalDb;
    private CustomerService customerService;

    @BeforeEach
    public void init() {
        setupCustomerService();
        initializeIdentityService();
        this.context = new FakeContext();

        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @AfterEach
    public void close() {
        this.customerServiceLocalDb.deleteDatabase();
        this.identityServiceLocalDb.closeDB();
    }

    @Test
    void shouldCreateTheDefaultRolesForTheService() throws IOException {

        var handler = new IdentityServiceInitHandler(identityService, customerService, ROLE_SOURCE);
        handler.handleRequest(createRequest(), output, context);
        var response = GatewayResponse.fromOutputStream(output, RoleList.class);
        var allRoles = response.getBodyObject(RoleList.class);
        Set<AccessRight> accessRights = extractAllAccessRights(allRoles);

        var expectedRolesCount = ROLE_SOURCE.roles().size();
        assertThat(allRoles.getRoles(), hasSize(expectedRolesCount));

        var expectedAccessRightsCount = ACCESS_RIGHTS.size();
        assertThat(accessRights, hasSize(expectedAccessRightsCount));
    }

    @Test
    void shouldLogWarningWhenRoleCreationFails() throws IOException {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var role = invalidRole();
        RoleSource roleSourceContainingIllegalRoleName = () -> List.of(role);
        var handler = new IdentityServiceInitHandler(identityService, customerService,
                                                     roleSourceContainingIllegalRoleName);
        handler.handleRequest(createRequest(), output, context);
        assertThat(logger.getMessages(), containsString(MISSING_ROLE_NAME_ERROR));
    }

    private Set<AccessRight> extractAllAccessRights(RoleList allRoles) {
        return allRoles.getRoles().stream()
                   .map(RoleDto::getAccessRights)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toSet());
    }

    private RoleDto invalidRole() {
        var role = new RoleDto();
        role.setRoleName(null);
        role.setAccessRights(Collections.emptyList());
        return role;
    }

    @Test
    void shouldUpdateRoleIfAlreadyExists()
        throws InvalidInputException, ConflictException, IOException, NotFoundException {
        var role = RoleDto.newBuilder()
                       .withRoleName(RoleName.DOI_CURATOR)
                       .withAccessRights(List.of(MANAGE_DOI))
                       .build();
        identityService.addRole(role);

        var handler = new IdentityServiceInitHandler(identityService, customerService, ROLE_SOURCE);
        handler.handleRequest(createRequest(), output, context);

        var updatedRole = identityService.getRole(role);
        assertThat(updatedRole.getAccessRights(), containsInAnyOrder(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS));
    }

    @Test
    void shouldCreateSiktCustomer() throws NotFoundException, IOException {
        var handler = new IdentityServiceInitHandler(identityService, customerService, ROLE_SOURCE);
        handler.handleRequest(createRequest(), output, context);
        var customer = customerService.getCustomerByCristinId(SIKT_CRISTIN_ID);
        assertThat(customer, is(not(nullValue())));
        assertThat(customer.getCristinId(), is(equalTo(SIKT_CRISTIN_ID)));
    }

    @Test
    void shouldCreateSiktBackendClientDBRow() throws NotFoundException, IOException {
        var handler = new IdentityServiceInitHandler(identityService, customerService, ROLE_SOURCE);
        handler.handleRequest(createRequest(), output, context);
        var client = identityService.getClient(ClientDto.newBuilder().withClientId(BACKEND_CLIENT_ID).build());
        assertThat(client, is(not(nullValue())));
        assertThat(client.getActingUser(), is(equalTo(SIKT_ACTING_USER)));
    }

    @Test
    void shouldNotCreateDuplicateSiktBackendClientDBRow() throws IOException {
        var handler = new IdentityServiceInitHandler(identityService, customerService, ROLE_SOURCE);
        handler.handleRequest(createRequest(), output, context);
        handler.handleRequest(createRequest(), output, context);
        verify(identityService, atMostOnce()).addExternalClient(any());
    }

    private void initializeIdentityService() {
        this.identityServiceLocalDb = new LocalIdentityService();
        this.identityServiceLocalDb.initializeTestDatabase();
        this.identityService = spy(new IdentityServiceImpl(this.identityServiceLocalDb.getDynamoDbClient()));
    }

    private void setupCustomerService() {
        this.customerServiceLocalDb = new LocalCustomerServiceDatabase();
        this.customerServiceLocalDb.setupDatabase();
        this.customerService = new DynamoDBCustomerService(customerServiceLocalDb.getDynamoClient());
    }

    private InputStream createRequest() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }
}