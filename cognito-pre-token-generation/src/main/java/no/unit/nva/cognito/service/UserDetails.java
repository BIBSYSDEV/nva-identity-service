package no.unit.nva.cognito.service;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.UserAttributes;
import nva.commons.core.StringUtils;

public class UserDetails {

    public static final URI NO_CUSTOMER_INFO = null;
    private static final String NO_CUSTOMER_INFO_STRING = null;
    private final String feideId;
    private final String givenName;
    private final String familyName;
    private final URI customerId;
    private final String affiliation;
    private final String cristinId;
    private final String cognitoUserName;
    private final String cognitoUserPool;

    public UserDetails(Event event, CustomerResponse customer) {
        UserAttributes userAttributes = event.getRequest().getUserAttributes();
        this.feideId = userAttributes.getFeideId();
        this.affiliation = nonBlankOrNull(userAttributes.getAffiliation());
        this.customerId = extractCustomerId(customer);
        this.cristinId = extractCristinId(customer);
        this.givenName = userAttributes.getGivenName();
        this.familyName = userAttributes.getFamilyName();
        this.cognitoUserName = event.getUserName();
        this.cognitoUserPool = event.getUserPoolId();
    }

    public UserDetails(Event event) {
        UserAttributes userAttributes = event.getRequest().getUserAttributes();
        this.feideId = userAttributes.getFeideId();
        this.affiliation = nonBlankOrNull(userAttributes.getAffiliation());
        this.customerId = NO_CUSTOMER_INFO;
        this.cristinId = NO_CUSTOMER_INFO_STRING;
        this.givenName = userAttributes.getGivenName();
        this.familyName = userAttributes.getFamilyName();
        this.cognitoUserPool = event.getUserPoolId();
        this.cognitoUserName = event.getUserName();
    }

    public String getCognitoUserName() {
        return cognitoUserName;
    }

    public String getCognitoUserPool() {
        return cognitoUserPool;
    }

    public Optional<String> getCristinId() {
        return Optional.ofNullable(cristinId);
    }

    public String getFeideId() {
        return feideId;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Optional<URI> getCustomerId() {
        return Optional.ofNullable(customerId);
    }

    public String getAffiliation() {
        return affiliation;
    }

    private String extractCristinId(CustomerResponse customer) {
        return Optional.ofNullable(customer).map(CustomerResponse::getCristinId).orElse(null);
    }

    private URI extractCustomerId(CustomerResponse customer) {
        return Optional.ofNullable(customer).map(CustomerResponse::getCustomerId).orElse(null);
    }

    private String nonBlankOrNull(String expectedNonBlank) {
        return StringUtils.isNotBlank(expectedNonBlank) ? expectedNonBlank : null;
    }
}
