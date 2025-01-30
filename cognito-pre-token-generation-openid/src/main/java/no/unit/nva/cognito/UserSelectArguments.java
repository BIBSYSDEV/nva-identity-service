package no.unit.nva.cognito;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.person.Person;

import java.net.URI;
import java.util.List;
import java.util.Set;

public record UserSelectArguments(
    AuthenticationDetails authenticationDetails,
    Person person,
    CustomerDto currentCustomer,
    UserDto currentUser,
    String impersonatedBy,
    URI currentTerms,
    URI acceptedTerms,
    String allowedCustomersString,
    List<UserDto> users,
    Set<CustomerDto> customers,
    List<String> accessRights,
    Set<RoleName> roles,
    Set<URI> viewingScopeIncluded,
    Set<URI> viewingScopeExcluded
) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return builder()
                   .withAuthenticationDetails(authenticationDetails)
                   .withPerson(person)
                   .withCurrentCustomer(currentCustomer)
                   .withCurrentUser(currentUser)
                   .withImpersonatedBy(impersonatedBy)
                   .withCurrentTerms(currentTerms)
                   .withAcceptedTerms(acceptedTerms)
                   .withAllowedCustomersString(allowedCustomersString)
                   .withCustomers(customers)
                   .withAccessRights(accessRights)
                   .withRoles(roles)
                   .withUsers(users)
                   .withViewingScopeIncluded(viewingScopeIncluded)
                   .withViewingScopeExcluded(viewingScopeExcluded);
    }

    public static class Builder {

        private AuthenticationDetails authenticationDetails;
        private Person person;
        private CustomerDto currentCustomer;
        private UserDto currentUser;
        private String impersonatedBy;
        private URI currentTerms;
        private URI acceptedTerms;
        private String allowedCustomersString;
        private List<UserDto> users;
        private Set<CustomerDto> customers;
        private List<String> accessRights;
        private Set<RoleName> roles;
        private Set<URI> viewingScopeIncluded;
        private Set<URI> viewingScopeExcluded;

        public Builder withAuthenticationDetails(AuthenticationDetails authenticationDetails) {
            this.authenticationDetails = authenticationDetails;
            return this;
        }

        public Builder withPerson(Person person) {
            this.person = person;
            return this;
        }

        public Builder withCurrentCustomer(CustomerDto customerDto) {
            this.currentCustomer = customerDto;
            return this;
        }

        public Builder withCurrentUser(UserDto userDto) {
            this.currentUser = userDto;
            return this;
        }

        public Builder withImpersonatedBy(String impersonatedBy) {
            this.impersonatedBy = impersonatedBy;
            return this;
        }

        public Builder withCurrentTerms(URI currentTerms) {
            this.currentTerms = currentTerms;
            return this;
        }

        public Builder withAcceptedTerms(URI acceptedTerms) {
            this.acceptedTerms = acceptedTerms;
            return this;
        }

        public Builder withCustomers(Set<CustomerDto> customers) {
            this.customers = customers;
            return this;
        }

        public Builder withAccessRights(List<String> accessRights) {
            this.accessRights = accessRights;
            return this;
        }

        public Builder withRoles(Set<RoleName> roles) {
            this.roles = roles;
            return this;
        }

        public Builder withUsers(List<UserDto> users) {
            this.users = users;
            return this;
        }

        public Builder withAllowedCustomersString(String allowedCustomersString) {
            this.allowedCustomersString = allowedCustomersString;
            return this;
        }

        public Builder withViewingScopeIncluded(Set<URI> viewingScopeIncluded) {
            this.viewingScopeIncluded = viewingScopeIncluded;
            return this;
        }

        public Builder withViewingScopeExcluded(Set<URI> viewingScopeExcluded) {
            this.viewingScopeExcluded = viewingScopeExcluded;
            return this;
        }

        public UserSelectArguments build() {
            return new UserSelectArguments(authenticationDetails, person, currentCustomer, currentUser, impersonatedBy,
                                           currentTerms, acceptedTerms, allowedCustomersString, users, customers,
                                           accessRights, roles,
                                           viewingScopeIncluded, viewingScopeExcluded);
        }
    }
}