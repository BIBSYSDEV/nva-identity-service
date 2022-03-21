package no.unit.nva.handlers;

import static no.unit.nva.handlers.CustomerSelectionHandler.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.handlers.CustomerSelectionHandler.AUTHORIZATION_HEADER;
import static no.unit.nva.handlers.CustomerSelectionHandler.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.FakeCognito;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;

class CustomerSelectionHandlerTest {

    public static final String MULTI_VALUE_CLAIMS_DELIMITER = ",";
    private final FakeContext context = new FakeContext();
    private FakeCognito cognito;
    private String accessToken;
    private Set<URI> allowedCustomers;

    @BeforeEach
    public void init() {
        cognito = new FakeCognito(randomString());
        allowedCustomers = Set.of(randomUri(), randomUri(), randomUri(), randomUri());
        var user = createUser(allowedCustomers);
        accessToken = randomString();
        cognito.addUser(accessToken, user);
    }

    @Test
    void shouldSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions() {
        var handler = new CustomerSelectionHandler(cognito);
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        var response = handler.handleRequest(input, context);
        var updatedSelectedCustomer = extractSelectedCustomerClaim();

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(URI.create(updatedSelectedCustomer), is(in(allowedCustomers.toArray(URI[]::new))));
    }

    @Test
    void shouldNotSendAnUpdateCustomerRequestToCognitoWhenInputContainsAnAccessTokenAndSelectionIsAmongTheValidOptions() {
        var handler = new CustomerSelectionHandler(cognito);
        var input = createRequest(randomUri());
        var response = handler.handleRequest(input, context);
        assertThatUpdateRequestHasNotBeenSent();
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldNotUserAdminRightsButOnlyAccessTokenBasedAccessToCognito() {
        var handler = new CustomerSelectionHandler(cognito);
        var selectedCustomer = randomElement(allowedCustomers.toArray(URI[]::new));
        var input = createRequest(selectedCustomer);
        handler.handleRequest(input, context);
        assertThat(cognito.getAdminUpdateUserRequest(), is(nullValue()));
        assertThat(cognito.getUpdateUserAttributesRequest(), is(not(nullValue())));
    }

    private GetUserResponse createUser(Set<URI> allowedCustomers) {
        var allowedCustomersString = allowedCustomers.stream()
            .map(URI::toString)
            .collect(Collectors.joining(MULTI_VALUE_CLAIMS_DELIMITER));
        var userAttribute = AttributeType.builder()
            .name(ALLOWED_CUSTOMERS_CLAIM)
            .value(allowedCustomersString)
            .build();
        return GetUserResponse.builder()
            .userAttributes(userAttribute)
            .build();
    }

    private void assertThatUpdateRequestHasNotBeenSent() {
        assertThat(cognito.getUpdateUserAttributesRequest(), is(nullValue()));
    }

    private String extractSelectedCustomerClaim() {
        return cognito.getUpdateUserAttributesRequest()
            .userAttributes().stream()
            .filter(a -> CURRENT_CUSTOMER_CLAIM.equals(a.name()))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
    }

    private APIGatewayProxyRequestEvent createRequest(URI customerId) {
        String randomCustomer = CustomerSelection.fromCustomerId(customerId).toString();
        return new APIGatewayProxyRequestEvent()
            .withHeaders(Map.of(AUTHORIZATION_HEADER, bearerToken()))
            .withBody(randomCustomer);
    }

    private String bearerToken() {
        return "Bearer " + accessToken;
    }
}