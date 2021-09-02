package no.unit.nva.cognito.service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.List;
import no.unit.nva.cognito.Constants;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPoolEntryUpdater {

    private static final Logger logger = LoggerFactory.getLogger(UserPoolEntryUpdater.class);
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    @JacocoGenerated
    public UserPoolEntryUpdater() {
        this(defaultCognitoProvider());
    }

    public UserPoolEntryUpdater(AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    /**
     * Add attributes to user.
     *
     * @param userPoolId userPoolId
     * @param userName   userName
     * @param attributes attributes
     */
    public void updateUserAttributes(String userPoolId, String userName, List<AttributeType> attributes) {
        AdminUpdateUserAttributesRequest request = new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withUserAttributes(attributes);
        logger.info("Updating User Attributes: " + request.toString());
        awsCognitoIdentityProvider.adminUpdateUserAttributes(request);
    }

    @JacocoGenerated
    private static AWSCognitoIdentityProvider defaultCognitoProvider() {
        return AWSCognitoIdentityProviderClientBuilder
            .standard()
            .withRegion(Constants.AWS_REGION_VALUE.getName())
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .build();
    }
}
