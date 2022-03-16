package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.TriggerHandler.COMMA_DELIMITER;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.cognito.Constants;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

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
    private final CognitoIdentityProviderClient awsCognitoIdentityProvider;

    @JacocoGenerated
    public UserPoolEntryUpdater() {
        this(defaultCognitoProvider());
    }

    public UserPoolEntryUpdater(CognitoIdentityProviderClient awsCognitoIdentityProvider) {
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
    private static CognitoIdentityProviderClient defaultCognitoProvider() {
        return CognitoIdentityProviderClient
            .builder()
            .httpClient(UrlConnectionHttpClient.create())
            .region(Constants.AWS_REGION)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    private AdminUpdateUserAttributesRequest createUpateRequestForUserEntryInCognito(
        UserDetails userDetails,
        List<AttributeType> userAttributes) {
        return AdminUpdateUserAttributesRequest
            .builder()
            .userPoolId(userDetails.getCognitoUserPool())
            .username(userDetails.getCognitoUserName())
            .userAttributes(userAttributes)
            .build();
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
        return AttributeType.builder()
            .name(name)
            .value(value)
            .build();
    }

    private <T> String toCsv(Collection<T> roles, Function<T, String> stringRepresentation) {
        return roles
            .stream()
            .map(stringRepresentation)
            .collect(Collectors.joining(COMMA_DELIMITER));
    }
}
