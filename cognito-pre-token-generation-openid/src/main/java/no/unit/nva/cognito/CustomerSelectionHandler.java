package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.CustomerSelection;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static nva.commons.core.attempt.Try.attempt;

public class CustomerSelectionHandler extends CognitoCommunicationHandler<CustomerSelection, Void> {

    private final CognitoIdentityProviderClient cognito;
    private final CustomerService customerService;
    private final IdentityService identityService;
    private final TermsAndConditionsService termsService;

    @JacocoGenerated
    public CustomerSelectionHandler() {
        this(defaultCognitoClient(),
             defaultCustomerService(DEFAULT_DYNAMO_CLIENT),
             IdentityService.defaultIdentityService(DEFAULT_DYNAMO_CLIENT),
             new TermsAndConditionsService(),
             new Environment()
        );
    }

    public CustomerSelectionHandler(CognitoIdentityProviderClient cognito,
                                    CustomerService customerService,
                                    IdentityService identityService,
                                    TermsAndConditionsService termsAndConditionsService, Environment environment) {
        super(CustomerSelection.class, environment);
        this.cognito = cognito;
        this.customerService = customerService;
        this.identityService = identityService;
        this.termsService = termsAndConditionsService;
    }

    @Override
    protected void validateRequest(CustomerSelection customerSelection, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(CustomerSelection input, RequestInfo event, Context context)
        throws ForbiddenException, UnauthorizedException {

        var accessToken = extractAccessToken(event);
        var userAttributes = fetchUserInfo(accessToken).userAttributes();
        validateInput(userAttributes, input.getCustomerId());
        updateCognitoUserEntryAttributes(input, userAttributes, accessToken, hasAcceptedTerms(event));
        return null;
    }

    private boolean hasAcceptedTerms(RequestInfo event) throws UnauthorizedException {
        return Optional.ofNullable(termsService.getTermsAndConditionsByPerson(event.getPersonCristinId()))
                   .map(TermsConditionsResponse::termsConditionsUri)
                   .map(userTerms -> termsService.getCurrentTermsAndConditions().termsConditionsUri().equals(userTerms))
                   .orElse(false);
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerSelection body, Void output) {
        return HttpURLConnection.HTTP_OK;
    }

    private GetUserResponse fetchUserInfo(String accessToken) {
        return cognito.getUser(GetUserRequest.builder().accessToken(accessToken).build());
    }

    private void updateCognitoUserEntryAttributes(CustomerSelection customerSelection,
                                                  List<AttributeType> userAttributes,
                                                  String accessToken,
                                                  boolean hasAcceptedTerms) {
        var user = fetchUser(customerSelection, userAttributes);
        var customer = attempt(() -> customerService.getCustomer(customerSelection.getCustomerId())).orElseThrow();
        var activeAccessRights = getActiveAccessRights(user, customer, hasAcceptedTerms);
        var selectedCustomerCustomClaim = createCustomerSelectionClaim(user);
        var rolesClaim = createRoleClaim(getActiveRoles(user, hasAcceptedTerms));
        var accessRightsClaim = createAccessRightsClaim(activeAccessRights);
        var nvaUsernameClaim = createUsernameClaim(user);
        var selectedCustomerCristinId = crateCustomerCristinIdClaim(user);
        var userAffiliation = createUserAffiliationClaim(user);

        var request = UpdateUserAttributesRequest.builder()
                          .accessToken(accessToken)
                          .userAttributes(selectedCustomerCustomClaim, nvaUsernameClaim, selectedCustomerCristinId,
                                          userAffiliation, accessRightsClaim, rolesClaim)
                          .build();
        cognito.updateUserAttributes(request);
    }

    private String getActiveRoles(UserDto user, boolean hasAcceptedTerms) {
        var roles = user.getRoles().stream()
                        .map(RoleDto::getRoleName)
                        .map(RoleName::getValue)
                        .collect(Collectors.joining(ELEMENTS_DELIMITER));
        return hasAcceptedTerms ? roles : EMPTY_CLAIM;
    }

    private String getActiveAccessRights(UserDto user, CustomerDto customer, Boolean hasAcceptedTerms) {
        return attempt(() -> UserAccessRightForCustomer.fromUser(user, Set.of(customer), hasAcceptedTerms))
                   .orElseThrow()
                   .stream()
                   .filter(ac -> ac.getCustomer().getId().equals(user.getInstitution()))
                   .map(UserAccessRightForCustomer::getAccessRight)
                   .map(AccessRight::toPersistedString)
                   .collect(Collectors.joining(ELEMENTS_DELIMITER));
    }

    private AttributeType createUserAffiliationClaim(UserDto user) {
        return createAttribute(PERSON_AFFILIATION_CLAIM, user.getAffiliation());
    }

    private AttributeType createRoleClaim(String claims) {
        return createAttribute(ROLES_CLAIM, claims);
    }

    private AttributeType createAttribute(String attributeName, String attributeValue) {
        return AttributeType.builder().name(attributeName)
                   .value(attributeValue)
                   .build();
    }

    private AttributeType createAccessRightsClaim(String claims) {
        return createAttribute(ACCESS_RIGHTS_CLAIM, claims);
    }

    private AttributeType crateCustomerCristinIdClaim(UserDto userDto) {
        return createAttribute(TOP_ORG_CRISTIN_ID, userDto.getInstitutionCristinId());
    }

    private AttributeType createUsernameClaim(UserDto user) {
        return createAttribute(NVA_USERNAME_CLAIM, user.getUsername());
    }

    private AttributeType createCustomerSelectionClaim(UserDto user) {
        return createAttribute(CURRENT_CUSTOMER_CLAIM, user.getInstitution());
    }

    private AttributeType createAttribute(String attributeName, URI attributeValue) {
        return AttributeType.builder().name(attributeName)
                   .value(attributeValue.toString())
                   .build();
    }

    private UserDto fetchUser(CustomerSelection customerSelection, List<AttributeType> userAttributes) {
        var cristinPersonId = extractCristinPersonId(userAttributes);
        var customerCristinId =
            fetchCustomerCristinId(customerSelection);
        return identityService.getUserByPersonCristinIdAndCustomerCristinId(cristinPersonId, customerCristinId);
    }

    private URI extractCristinPersonId(List<AttributeType> userAttributes) {
        return userAttributes.stream()
                   .filter(attribute -> PERSON_ID_CLAIM.equals(attribute.name()))
                   .map(AttributeType::value)
                   .map(URI::create)
                   .collect(SingletonCollector.collect());
    }

    private URI fetchCustomerCristinId(CustomerSelection customerSelection) {
        return attempt(() -> customerService.getCustomer(customerSelection.getCustomerId()).getCristinId())
                   .orElseThrow();
    }

    private void validateInput(List<AttributeType> userAttributes, URI customerId) throws ForbiddenException {
        var allowedCustomers = extractAllowedCustomers(userAttributes);
        var desiredCustomerIdString = customerId.toString().toLowerCase(Locale.getDefault());
        if (desiredCustomerIsNotAllowed(allowedCustomers, desiredCustomerIdString)) {
            throw new ForbiddenException();
        }
    }

    private boolean desiredCustomerIsNotAllowed(String allowedCustomers, String desiredCustomerIdString) {
        return !allowedCustomers.toLowerCase(Locale.getDefault()).contains(desiredCustomerIdString);
    }

    private String extractAllowedCustomers(List<AttributeType> userAttributes) {
        return userAttributes.stream()
                   .filter(attribute -> ALLOWED_CUSTOMERS_CLAIM.equals(attribute.name()))
                   .collect(SingletonCollector.tryCollect())
                   .map(AttributeType::value)
                   .orElseThrow();
    }
}
