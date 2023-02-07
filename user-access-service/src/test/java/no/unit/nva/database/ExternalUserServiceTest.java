package no.unit.nva.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

class ExternalUserServiceTest {

    private CognitoIdentityProviderClient cognitoClient;

    public static final String CLIENT_NAME = "client1";
    public static final String CLIENT_ID= "id1";
    public static final String CLIENT_SECRET = "secret1";
    private static final String EXTERNAL_USER_POOL_URL = new Environment().readEnv("EXTERNAL_USER_POOL_URL");
    private ExternalUserService service;

    @BeforeEach
    public void setup()  {
        cognitoClient = Mockito.mock(CognitoIdentityProviderClient.class);
        service = new ExternalUserService(cognitoClient);

        var response = CreateUserPoolClientResponse.builder().userPoolClient(
            UserPoolClientType.builder().clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).build()
        ).build();

        Mockito.when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenReturn(response);
    }

    @Test
    public void shouldReturnCredentials() {
        var response = service.createNewExternalUserClient(CLIENT_NAME);

        assertThat(response.getClientId(),  is(equalTo(CLIENT_ID)));
        assertThat(response.getClientSecret(),  is(equalTo(CLIENT_SECRET)));
        assertThat(response.getClientUrl(),  is(equalTo(EXTERNAL_USER_POOL_URL)));
    }

}