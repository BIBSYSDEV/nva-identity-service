package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.BACKEND_USER_POOL_CLIENT_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

public class FakeCognitoIdentityProviderClient implements CognitoIdentityProviderClient {

    private final String fakeClientSecret = randomString();
    private final String fakeClientId = randomString();

    public String getFakeClientId() {
        return fakeClientId;
    }

    public String getFakeClientSecret() {
        return fakeClientSecret;
    }

    @Override
    public String serviceName() {
        return FakeCognitoIdentityProviderClient.class.getName();
    }

    @Override
    public void close() {

    }

    @Override
    public DescribeUserPoolClientResponse describeUserPoolClient(
        DescribeUserPoolClientRequest describeUserPoolClientRequest) {
        var userPoolClient = UserPoolClientType.builder()
            .clientId(describeUserPoolClientRequest.clientId())
            .clientSecret(fakeClientSecret)
            .build();
        return DescribeUserPoolClientResponse.builder().userPoolClient(userPoolClient).build();
    }

    @Override
    public ListUserPoolClientsResponse listUserPoolClients(ListUserPoolClientsRequest request) {

        UserPoolClientDescription userPoolClient = UserPoolClientDescription.builder()
            .clientId(fakeClientId)
            .clientName(BACKEND_USER_POOL_CLIENT_NAME)
            .build();
        return ListUserPoolClientsResponse.builder().userPoolClients(userPoolClient).build();
    }
}
