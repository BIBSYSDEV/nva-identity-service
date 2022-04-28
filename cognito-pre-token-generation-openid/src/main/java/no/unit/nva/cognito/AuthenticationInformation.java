package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.SingletonCollector;

public class AuthenticationInformation {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideIdNin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideId";
    public static final String ORG_FEIDE_DOMAIN = "custom:orgFeideDomain";
    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";

    private final String nationalIdentityNumber;
    private final String feideIdentifier;
    private final String orgFeideDomain;
    private CristinPersonResponse cristinResponse;
    private Set<CustomerDto> activeCustomers;
    private CustomerDto currentCustomer;
    private List<PersonAffiliation> personAffiliations;
    private UserDto currentUser;
    private String userPoolId;

    public AuthenticationInformation(String nin, String feideIdentifier, String orgFeideDomain) {

        this.nationalIdentityNumber = nin;
        this.feideIdentifier = feideIdentifier;
        this.orgFeideDomain = orgFeideDomain;
    }

    public static AuthenticationInformation create(CognitoUserPoolPreTokenGenerationEvent input) {
        var nin = extractNin(input.getRequest().getUserAttributes());
        var feideIdentifier = extractFeideIdentifier(input.getRequest().getUserAttributes());
        var orgFeideDomain = extractOrgFeideDomain(input.getRequest().getUserAttributes());
        var userPoolId = input.getUserPoolId();
        var authenticationInfo = new AuthenticationInformation(nin, feideIdentifier, orgFeideDomain);
        authenticationInfo.setUserPoolId(userPoolId);
        return authenticationInfo;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    private void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
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

    public URI getOrganizationAffiliation(URI parentInstitution) {
        var affiliations = allAffiliationsWithSameParentInstitution(parentInstitution);
        return anyAffiliationButProduceConsistentResponseForSameInputSet(affiliations);
    }

    public List<PersonAffiliation> getPersonAffiliations() {
        return this.personAffiliations;
    }

    public void setPersonAffiliations(List<PersonAffiliation> affiliationInformation) {
        this.personAffiliations = affiliationInformation;
    }

    public UserDto getCurrentUser() {
        return currentUser;
    }

    public void updateCurrentUser(Collection<UserDto> users) {
        if (nonNull(currentCustomer)) {
            var currentCustomerId = currentCustomer.getId();
            this.currentUser = attempt(() -> filterOutUser(users, currentCustomerId))
                .orElseThrow(fail -> handleUserNotFoundError());
        }
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

    private Stream<URI> allAffiliationsWithSameParentInstitution(URI parentInstitution) {
        return this.personAffiliations.stream()
            .filter(affiliation -> affiliation.getParentInstitution().equals(parentInstitution))
            .map(PersonAffiliation::getOrganization);
    }

    private URI anyAffiliationButProduceConsistentResponseForSameInputSet(Stream<URI> affiliations) {
        return affiliations.map(URI::toString).sorted().map(URI::create).findFirst().orElseThrow();
    }

    private IllegalStateException handleUserNotFoundError() {
        return new IllegalStateException(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR + currentCustomer.getId());
    }

    private UserDto filterOutUser(Collection<UserDto> users, URI currentCustomerId) {
        return users.stream()
            .filter(user -> user.getInstitution().equals(currentCustomerId))
            .collect(SingletonCollector.collect());
    }

    private CustomerDto returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer() {
        return activeCustomers.stream()
            .filter(this::selectFeideOrgIfApplicable)
            .collect(SingletonCollector.tryCollect())
            .orElse(fail -> null);
    }

    private boolean selectFeideOrgIfApplicable(CustomerDto customer) {
        return userLoggedInWithNin()
               || getOrgFeideDomain().equals(customer.getFeideOrganizationDomain());
    }
}
