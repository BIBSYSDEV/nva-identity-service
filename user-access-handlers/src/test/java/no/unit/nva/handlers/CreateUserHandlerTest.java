package no.unit.nva.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.accessrights.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.useraccessservice.accessrights.AccessRight;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class CreateUserHandlerTest extends HandlerTest {

    private CreateUserHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        handler = new CreateUserHandler();
        context = new FakeContext();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldAcceptRequestContainingPersonCristinIdCustomerIdAndListOfRoles() throws IOException {
        var requestBody = sampleRequest();
        var request = createRequest(requestBody, EDIT_OWN_INSTITUTION_USERS);
        GatewayResponse<UserDto> response = sendRequest(request, UserDto.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var actualUser = response.getBodyObject(UserDto.class);
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
    }

    @Test
    void shouldDenyAccessToUsersThatAreDoNotHaveTheRightToCreateUsers() throws IOException {
        var requestBody = sampleRequest();
        var request = createRequestWithoutAccessRights(requestBody);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldDenyAccessToUsersThatDoNotHaveTheRightToAddAnyUserAndTheyAreTryingToAddUsersForAnotherInstitution()
        throws IOException {
        var requestBody = sampleRequest();
        var request = createRequestWithoutAccessRights(requestBody);
        var response = sendRequest(request, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    private <T> GatewayResponse<T> sendRequest(InputStream request, Class<T> responseType) throws IOException {
        handler.handleRequest(request, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, responseType);
    }

    private InputStream createRequestWithoutAccessRights(CreateUserRequest requestBody) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCustomerId(customerId)
            .withAccessRights(customerId, randomString())
            .withBody(requestBody)
            .build();
    }

    private InputStream createRequest(CreateUserRequest requestBody, AccessRight... accessRights)
        throws JsonProcessingException {
        var customerId = randomUri();
        var accessRightStrings = Arrays.stream(accessRights)
            .map(AccessRight::toString)
            .toArray(String[]::new);
        return new HandlerRequestBuilder<CreateUserRequest>(dtoObjectMapper)
            .withCustomerId(customerId)
            .withAccessRights(customerId, accessRightStrings)
            .withBody(requestBody)
            .build();
    }

    private CreateUserRequest sampleRequest() {
        var nin = new NationalIdentityNumber(randomString());
        var customerId = randomUri();
        var roles = randomRoles();
        return new CreateUserRequest(nin, customerId, roles);
    }

    private List<RoleDto> randomRoles() {
        var role = RoleDto.newBuilder().withRoleName(randomString()).build();
        return List.of(role);
    }
}
