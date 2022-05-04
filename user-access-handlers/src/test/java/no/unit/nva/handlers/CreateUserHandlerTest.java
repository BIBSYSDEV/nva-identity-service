package no.unit.nva.handlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateUserHandlerTest extends HandlerTest {

    private CreateUserHandler handler;
    private Context context;

    @BeforeEach
    public void init() {
        handler = new CreateUserHandler();
        context = new FakeContext();
    }

    @Test
    void shouldAcceptRequestContainingPersonCristinIdCustomerIdAndListOfRoles() {
        var requestBody = sampleRequest();
        var request = new APIGatewayProxyRequestEvent().withBody(requestBody.toString());
        var response = handler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));

        var actualUser = UserDto.fromJson(response.getBody());
        assertThat(actualUser.getInstitution(), is(equalTo(requestBody.getCustomerId())));
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
