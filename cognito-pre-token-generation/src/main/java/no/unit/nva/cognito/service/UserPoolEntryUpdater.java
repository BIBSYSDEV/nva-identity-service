package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.TriggerHandler.COMMA_DELIMITER;
import static no.unit.nva.cognito.TriggerHandler.EMPTY_STRING;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.cognito.Constants;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPoolEntryUpdater {

    public static final String CUSTOM_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_APPLICATION = "custom:application";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_IDENTIFIERS = "custom:identifiers";
    public static final String CUSTOM_CRISTIN_ID = "custom:cristinId";
    public static final String CUSTOM_APPLICATION_ACCESS_RIGHTS = "custom:accessRights";
    public static final String APPLICATION_ROLES_MESSAGE = "applicationRoles: ";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";
    public static final String COGNITO_UPDATE_FAILURE_WARNING = "The following attributes have not been registered:";
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
     */
    public void updateUserAttributes(UserDetails userDetails, UserDto user) {

        List<AttributeType> userAttributes = createUserAttributes(userDetails, user);
        AdminUpdateUserAttributesRequest request = createUpateRequestForUserEntryInCognito(userDetails, userAttributes);
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

    private AdminUpdateUserAttributesRequest createUpateRequestForUserEntryInCognito(
        UserDetails userDetails,
        List<AttributeType> userAttributes) {
        return new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userDetails.getCognitoUserPool())
            .withUsername(userDetails.getCognitoUserName())
            .withUserAttributes(userAttributes);
    }

    private List<AttributeType> createUserAttributes(UserDetails userDetails, UserDto user) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        if (user.getInstitution() != null) {
            userAttributeTypes.add(toAttributeType(CUSTOM_CUSTOMER_ID, user.getInstitution().toString()));
        }
        userDetails.getCristinId()
            .ifPresent(cristinId -> userAttributeTypes.add(toAttributeType(CUSTOM_CRISTIN_ID, cristinId)));

        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + userDetails.getFeideId()));

        String applicationRoles = applicationRolesString(user);
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION_ROLES, applicationRoles));

        String accessRightsString = accessRightsString(user);
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION_ACCESS_RIGHTS, accessRightsString));

        return userAttributeTypes;
    }

    private String accessRightsString(UserDto user) {
        if (!user.getAccessRights().isEmpty()) {
            return toCsv(user.getAccessRights(), s -> s);
        } else {
            return EMPTY_STRING;
        }
    }

    private String applicationRolesString(UserDto user) {
        String applicationRoles = toCsv(user.getRoles(), RoleDto::getRoleName);
        logger.info(APPLICATION_ROLES_MESSAGE + applicationRoles);
        return applicationRoles;
    }

    private AttributeType toAttributeType(String name, String value) {
        AttributeType attributeType = new AttributeType();
        attributeType.setName(name);
        attributeType.setValue(value);
        return attributeType;
    }

    private <T> String toCsv(Collection<T> roles, Function<T, String> stringRepresentation) {
        return roles
            .stream()
            .map(stringRepresentation)
            .collect(Collectors.joining(COMMA_DELIMITER));
    }
}
