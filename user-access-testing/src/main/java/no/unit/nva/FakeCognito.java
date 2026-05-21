package no.unit.nva;

import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;

public class FakeCognito implements CognitoIdentityProviderClient {

    private AdminUpdateUserAttributesRequest adminUpdateUserRequest;

    public AdminUpdateUserAttributesRequest getAdminUpdateUserRequest() {
        return adminUpdateUserRequest;
    }

    @JacocoGenerated
    @Override
    public String serviceName() {
        return FakeCognito.class.getName();
    }

    @JacocoGenerated
    @Override
    public void close() {

    }

    @Override
    public AdminUpdateUserAttributesResponse adminUpdateUserAttributes(AdminUpdateUserAttributesRequest request) {
        this.adminUpdateUserRequest = request;
        return AdminUpdateUserAttributesResponse.builder().build();
    }
}
