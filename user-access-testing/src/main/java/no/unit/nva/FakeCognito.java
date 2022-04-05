package no.unit.nva;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

public class FakeCognito implements CognitoIdentityProviderClient {

    private final String fakeClientSecret = randomString();
    private final String fakeClientId = randomString();
    private final String clientName;
    private final Map<String, GetUserResponse> users;

    private AdminUpdateUserAttributesRequest adminUpdateUserRequest;
    private UpdateUserAttributesRequest updateUserAttributesRequest;

    public FakeCognito(String clientName) {
        this.clientName = clientName;
        this.users = new ConcurrentHashMap<>();
    }

    public UpdateUserAttributesRequest getUpdateUserAttributesRequest() {
        return updateUserAttributesRequest;
    }

    public void addUser(String accessToken, GetUserResponse user) {
        users.put(accessToken, user);
    }

    public AdminUpdateUserAttributesRequest getAdminUpdateUserRequest() {
        return adminUpdateUserRequest;
    }

    public String getFakeClientId() {
        return fakeClientId;
    }

    public String getFakeClientSecret() {
        return fakeClientSecret;
    }

    @Override
    public String serviceName() {
        return FakeCognito.class.getName();
    }

    @Override
    public void close() {

    }

    @Override
    public AdminAddUserToGroupResponse adminAddUserToGroup(AdminAddUserToGroupRequest request) {
        return AdminAddUserToGroupResponse.builder().build();
    }

    @Override
    public AdminUpdateUserAttributesResponse adminUpdateUserAttributes(AdminUpdateUserAttributesRequest request) {
        this.adminUpdateUserRequest = request;
        return AdminUpdateUserAttributesResponse.builder().build();
    }

    @Override
    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        GroupType group = GroupType.builder()
            .userPoolId(request.userPoolId())
            .groupName(request.groupName())
            .build();
        return CreateGroupResponse.builder().group(group).build();
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
    public GetGroupResponse getGroup(GetGroupRequest request) {
        GroupType group = GroupType.builder()
            .userPoolId(request.userPoolId())
            .groupName(request.groupName())
            .build();
        return GetGroupResponse.builder().group(group).build();
    }

    @Override
    public GetUserResponse getUser(GetUserRequest getUserRequest) {
        return users.get(getUserRequest.accessToken());
    }

    @Override
    public ListUserPoolClientsResponse listUserPoolClients(ListUserPoolClientsRequest request) {

        UserPoolClientDescription userPoolClient = UserPoolClientDescription.builder()
            .clientId(fakeClientId)
            .clientName(clientName)
            .build();
        return ListUserPoolClientsResponse.builder().userPoolClients(userPoolClient).build();
    }

    @Override
    public UpdateUserAttributesResponse updateUserAttributes(UpdateUserAttributesRequest request) {
        this.updateUserAttributesRequest = request;
        return UpdateUserAttributesResponse.builder().build();
    }
}
