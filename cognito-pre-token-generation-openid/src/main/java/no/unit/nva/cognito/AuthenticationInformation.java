package no.unit.nva.cognito;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.PersonInformation;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.SingletonCollector;

public class AuthenticationInformation implements PersonInformation {

    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";
    private final PersonInformation personInformation;
    private CustomerDto currentCustomer;
    private UserDto currentUser;

    public AuthenticationInformation(PersonInformation personInformation) {
        this.personInformation = personInformation;
    }

    public CustomerDto getCurrentCustomer() {
        return currentCustomer;
    }

    public String extractFirstName() {
        return getCristinPersonResponse().extractFirstName();
    }

    public String extractLastName() {
        return getCristinPersonResponse().extractLastName();
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

    @Override
    public Set<CustomerDto> getActiveCustomers() {
        return personInformation.getActiveCustomers();
    }

    @Override
    public void setActiveCustomers(Set<CustomerDto> activeCustomers) {
        personInformation.setActiveCustomers(activeCustomers);
    }

    @Override
    public String getOrgFeideDomain() {
        return personInformation.getOrgFeideDomain();
    }

    @Override
    public String getPersonFeideIdentifier() {
        return personInformation.getPersonFeideIdentifier();
    }

    @Override
    public CristinPersonResponse getCristinPersonResponse() {
        return personInformation.getCristinPersonResponse();
    }

    @Override
    public void setCristinPersonResponse(CristinPersonResponse cristinResponse) {
        personInformation.setCristinPersonResponse(cristinResponse);
    }

    @Override
    public URI getOrganizationAffiliation(URI parentInstitution) {
        return personInformation.getOrganizationAffiliation(parentInstitution);
    }

    @Override
    public List<PersonAffiliation> getPersonAffiliations() {
        return personInformation.getPersonAffiliations();
    }

    @Override
    public void setPersonAffiliations(List<PersonAffiliation> affiliationInformation) {
        personInformation.setPersonAffiliations(affiliationInformation);
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
        return getActiveCustomers().stream()
            .filter(this::selectFeideOrgIfApplicable)
            .collect(SingletonCollector.tryCollect())
            .orElse(fail -> null);
    }

    private boolean selectFeideOrgIfApplicable(CustomerDto customer) {
        return userAuthenticatedWithNin()
               || getOrgFeideDomain().equals(customer.getFeideOrganizationDomain());
    }

    private boolean userAuthenticatedWithNin() {
        return isNull(getOrgFeideDomain());
    }
}
