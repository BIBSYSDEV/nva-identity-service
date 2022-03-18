package no.unit.nva.handlers;

import static no.unit.nva.handlers.UserSelectionHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.URI;
import java.util.Map;
import no.unit.nva.FakeCognito;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSelectionHandlerTest {

    private FakeCognito cognito;
    private final FakeContext context = new FakeContext();

    @BeforeEach
    public void init(){
        cognito = new FakeCognito(randomString());
    }

    @Test
    void shouldSendAnUpdateUserRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions(){
        var handler = new UserSelectionHandler(cognito);
        var input= createRequest(randomUri());
        handler.handleRequest(input, context);

    }

    private APIGatewayProxyRequestEvent createRequest(URI customerId) {
        String randomCustomer = CustomerSelection.fromCustomerId(customerId).toString();
        return  new APIGatewayProxyRequestEvent()
                    .withHeaders(Map.of(AUTHORIZATION_HEADER, bearerToken()))
                    .withBody(randomCustomer);

    }

    private String bearerToken() {
        return "Bearer "+randomString();
    }
}