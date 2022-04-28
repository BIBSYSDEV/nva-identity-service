package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.SingletonCollector;

public class AuthenticationInformation {

    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";

    private final String personFeideIdentifier;
    private final String orgFeideDomain;
    private CristinPersonResponse cristinResponse;
    private Set<CustomerDto> activeCustomers;
    private CustomerDto currentCustomer;
    private List<PersonAffiliation> personAffiliations;
    private UserDto currentUser;

    public AuthenticationInformation(String personFeideIdentifier, String orgFeideDomain) {
        this.personFeideIdentifier = personFeideIdentifier;
        this.orgFeideDomain = orgFeideDomain;
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

    public String getPersonFeideIdentifier() {
        return personFeideIdentifier;
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

    private boolean userAuthenticatedWithNin() {
        return isNull(orgFeideDomain);
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
        return userAuthenticatedWithNin()
               || getOrgFeideDomain().equals(customer.getFeideOrganizationDomain());
    }
}
