package no.unit.nva.cognito.service;

import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.UserAttributes;
import nva.commons.core.StringUtils;

public class UserDetails {

    public static final String NO_CUSTOMER_INFO = null;
    private final String feideId;
    private final String givenName;
    private final String familyName;
    private final String customerId;
    private final String affiliation;
    private final String cristinId;

    public UserDetails(UserAttributes userAttributes, CustomerResponse customer) {
        this.feideId = userAttributes.getFeideId();
        this.affiliation = nonBlankOrNull(userAttributes.getAffiliation());
        this.customerId = extractCustomerId(customer);
        this.cristinId = extractCristinId(customer);
        this.givenName = userAttributes.getGivenName();
        this.familyName = userAttributes.getFamilyName();
    }

    public UserDetails(UserAttributes userAttributes) {
        this.feideId = userAttributes.getFeideId();
        this.affiliation = nonBlankOrNull(userAttributes.getAffiliation());
        this.customerId = NO_CUSTOMER_INFO;
        this.cristinId = NO_CUSTOMER_INFO;
        this.givenName = userAttributes.getGivenName();
        this.familyName = userAttributes.getFamilyName();
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

    public Optional<String> getCustomerId() {
        return Optional.ofNullable(customerId);
    }

    public String getAffiliation() {
        return affiliation;
    }

    private String extractCristinId(CustomerResponse customer) {
        return Optional.ofNullable(customer).map(CustomerResponse::getCristinId).orElse(null);
    }

    private String extractCustomerId(CustomerResponse customer) {
        return Optional.ofNullable(customer).map(CustomerResponse::getCustomerId).orElse(null);
    }

    private String nonBlankOrNull(String expectedNonBlank) {
        return StringUtils.isNotBlank(expectedNonBlank) ? expectedNonBlank : null;
    }
}
