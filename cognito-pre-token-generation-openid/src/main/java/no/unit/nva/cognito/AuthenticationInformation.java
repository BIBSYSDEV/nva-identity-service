package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import nva.commons.core.SingletonCollector;

public class AuthenticationInformation {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideidnin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideid";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";

    private final String nationalIdentityNumber;
    private final String feideIdentifier;
    private final String orgFeideDomain;
    private CristinPersonResponse cristinResponse;
    private Set<CustomerDto> activeCustomers;
    private CustomerDto currentCustomer;
    private List<URI> topLevelOrganizations;

    public AuthenticationInformation(String nin, String feideIdentifier, String orgFeideDomain) {

        this.nationalIdentityNumber = nin;
        this.feideIdentifier = feideIdentifier;
        this.orgFeideDomain = orgFeideDomain;
    }

    public static AuthenticationInformation create(CognitoUserPoolPreTokenGenerationEvent input) {
        var nin = extractNin(input.getRequest().getUserAttributes());
        var feideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        return new AuthenticationInformation(nin, feideIdentifier, orgFeideDomain);
    }

    public boolean userLoggedInWithNin() {
        return isNull(orgFeideDomain);
    }

    public Set<CustomerDto> getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(Set<CustomerDto> activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public String getOrgFeideDomain() {
        return orgFeideDomain;
    }

    public String getNationalIdentityNumber() {
        return nationalIdentityNumber;
    }

    public String getFeideIdentifier() {
        return feideIdentifier;
    }

    public CristinPersonResponse getCristinPersonResponse() {
        return this.cristinResponse;
    }

    public CristinPersonResponse getCristinResponse() {
        return cristinResponse;
    }

    public void setCristinResponse(CristinPersonResponse cristinResponse) {
        this.cristinResponse = cristinResponse;
    }

    public CustomerDto getCurrentCustomer() {
        return currentCustomer;
    }

    public String extractFirstName() {
        return getCristinPersonResponse().extractFirstName();
    }

    public String extractLastName() {
        return getCristinResponse().extractLastName();
    }

    public Optional<String> getCurrentCustomerId() {
        return Optional.ofNullable(getCurrentCustomer()).map(CustomerDto::getId).map(URI::toString);
    }

    public URI getCristinPersonId() {
        return getCristinPersonResponse().getCristinId();
    }

    public CustomerDto updateCurrentCustomer() {
        this.currentCustomer = returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer();
        return currentCustomer;
    }

    public void setTopLevelOrganizations(List<URI> topLevelOrganizations) {
        this.topLevelOrganizations = topLevelOrganizations;
    }

    public List<URI> getTopLevelOrganizations() {
        return this.topLevelOrganizations;
    }

    private static String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
            .orElseThrow();
    }

    private static String extractOrgFeideDomain(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(ORG_FEIDE_DOMAIN)).orElse(null);
    }

    private static String extractFeideIdentifier(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(FEIDE_ID)).orElse(null);
    }

    private CustomerDto returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer() {
        return activeCustomers.stream()
            .filter(this::selectFeideOrgIfApplicable)
            .collect(SingletonCollector.tryCollect())
            .orElse(fail -> null);
    }

    private boolean selectFeideOrgIfApplicable(CustomerDto customer) {
        return userLoggedInWithNin() ||
               getOrgFeideDomain().equals(customer.getFeideOrganizationDomain());
    }
}
