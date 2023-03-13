package no.unit.nva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeResourceServerRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeResourceServerResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InternalErrorException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerScopeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ScopeDoesNotExistException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

class CognitoServiceTest {

    private CognitoService cognitoService;
    private CognitoIdentityProviderClient cognitoClient;
    public static final String CLIENT_NAME = "client1";
    public static final String CLIENT_ID = "id1";
    public static final String CLIENT_SECRET = "secret1";
    private static final String EXTERNAL_SCOPE_IDENTIFIER = new Environment().readEnv("EXTERNAL_SCOPE_IDENTIFIER");

    @BeforeEach
    public void setup() {
        cognitoClient = Mockito.mock(CognitoIdentityProviderClient.class);
        cognitoService = new CognitoService(cognitoClient);

        var createUserPoolClientResponse = CreateUserPoolClientResponse.builder().userPoolClient(
            UserPoolClientType.builder().clientId(CLIENT_ID).clientSecret(CLIENT_SECRET).build()
        ).build();

        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenReturn(createUserPoolClientResponse);

        var describeRequestServerResponse = DescribeResourceServerResponse
                                                .builder()
                                                .resourceServer(ResourceServerType
                                                                    .builder()
                                                                    .identifier(EXTERNAL_SCOPE_IDENTIFIER)
                                                                    .scopes(List.of(
                                                                        ResourceServerScopeType
                                                                            .builder()
                                                                            .scopeName("publication-read")
                                                                            .build()
                                                                    ))
                                                                    .build())
                                                .build();

        when(cognitoClient.describeResourceServer(any(DescribeResourceServerRequest.class)))
            .thenReturn(describeRequestServerResponse);
    }

    @Test
    void shouldNotThrowWhenCalledWithValidArguments() {
        Executable action = () -> cognitoService.createUserPoolClient(CLIENT_NAME, List.of());

        assertDoesNotThrow(action);
    }

    @Test
    void shouldReturnValidResponseWhenCalledWithValidArguments() throws BadRequestException {
        var createUserPoolClientResponse = cognitoService.createUserPoolClient(CLIENT_NAME, List.of());
        assertNotNull(createUserPoolClientResponse.userPoolClient());
    }

    @Test
    void shouldThrowBadReqeuestExceptionWhenCreatingCognitoClientWithUnknownScopes()
        throws InvalidEntryInternalException {

        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(ScopeDoesNotExistException.class);

        Executable action = () -> cognitoService.createUserPoolClient(CLIENT_NAME, List.of());

        assertThrows(BadRequestException.class, action);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenCreatingCognitoClientWithUnhandledException()
        throws InvalidEntryInternalException {

        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
            .thenThrow(InternalErrorException.class);

        Executable action = () -> cognitoService.createUserPoolClient(CLIENT_NAME, List.of());

        assertThrows(RuntimeException.class, action);
    }
}