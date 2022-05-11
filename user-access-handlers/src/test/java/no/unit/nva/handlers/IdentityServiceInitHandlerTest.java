package no.unit.nva.handlers;

import static no.unit.nva.database.RoleService.ROLE_ALREADY_EXISTS_ERROR_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityServiceInitHandlerTest extends LocalIdentityService {

    private static final int USER_PSEUDO_ACCESS_RIGHT = 1;
    private IdentityService identityService;
    private ByteArrayOutputStream output;
    private Context context;

    @BeforeEach
    public void init() {
        super.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(this.localDynamo);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @Test
    void createdDefaultRolesShouldCoverAllAccessRights() throws IOException {
        var handler = new IdentityServiceInitHandler(identityService);
        handler.handleRequest(createRequest(), output, context);
        var response = GatewayResponse.fromOutputStream(output, RoleList.class);
        var allRoles = response.getBodyObject(RoleList.class);
        var accessRights = allRoles.getRoles().stream()
            .map(RoleDto::getAccessRights)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        assertThat(accessRights.size(), is(equalTo(AccessRight.values().length - USER_PSEUDO_ACCESS_RIGHT)));
    }

    @Test
    void shouldLogWarningWhenRoleCreationFails() throws IOException {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var handler = new IdentityServiceInitHandler(identityService);
        handler.handleRequest(createRequest(), output, context);
        makeRequestFailDueToCollisionError(handler, createRequest());
        assertThat(logger.getMessages(), containsString(ROLE_ALREADY_EXISTS_ERROR_MESSAGE));
    }

    private InputStream createRequest() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }

    private void makeRequestFailDueToCollisionError(IdentityServiceInitHandler handler, InputStream request)
        throws IOException {
        handler.handleRequest(request, output, context);
    }
}