package no.unit.nva.cognito;

import static no.unit.nva.cognito.NetworkingUtils.BACKEND_CLIENT_NAME;
import static no.unit.nva.cognito.NetworkingUtils.USERPOOL_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;

public class FakeCognitoIdentityProviderClient implements CognitoIdentityProviderClient {

    private final String fakeClientSecret = randomString();

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
            .clientName(BACKEND_CLIENT_NAME)
            .clientId(randomString())
            .build();
        return ListUserPoolClientsResponse.builder().userPoolClients(userPoolClient).build();
    }

    @Override
    public ListUserPoolsResponse listUserPools(ListUserPoolsRequest listUserPoolsRequest)
        throws AwsServiceException, SdkClientException {
        var userPool = UserPoolDescriptionType.builder()
            .name(USERPOOL_NAME)
            .id(randomString())
            .build();
        return ListUserPoolsResponse.builder().userPools(userPool).build();
    }
}
