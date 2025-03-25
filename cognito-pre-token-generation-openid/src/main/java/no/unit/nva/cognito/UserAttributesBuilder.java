package no.unit.nva.cognito;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.CognitoClaims.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ALLOWED_CUSTOMERS_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_CUSTOMER_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.CURRENT_TERMS;
import static no.unit.nva.cognito.CognitoClaims.CUSTOMER_ACCEPTED_TERMS;
import static no.unit.nva.cognito.CognitoClaims.ELEMENTS_DELIMITER;
import static no.unit.nva.cognito.CognitoClaims.EMPTY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.FEIDE_ID;
import static no.unit.nva.cognito.CognitoClaims.FIRST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.IMPERSONATED_BY_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.LAST_NAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.NVA_USERNAME_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.ROLES_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.TOP_ORG_CRISTIN_ID;
import static no.unit.nva.cognito.CognitoClaims.VIEWING_SCOPE_EXCLUDED_CLAIM;
import static no.unit.nva.cognito.CognitoClaims.VIEWING_SCOPE_INCLUDED_CLAIM;

public class UserAttributesBuilder {

    private Person person;
    private URI currentTerms;
    private URI acceptedTerms;
    private List<UserDto> users;
    private Set<CustomerDto> customersForPerson;
    private CustomerDto currentCustomer;
    private AuthenticationDetails authenticationDetails;
    private String impersonatedBy;
    private UserDto currentUser;
    private List<String> accessRightsPersistedFormat;
    private static final String EMPTY_STRING = "";

    public UserAttributesBuilder withPerson(Person person) {
        this.person = person;
        return this;
    }

    public UserAttributesBuilder withCurrentTerms(URI currentTerms) {
        this.currentTerms = currentTerms;
        return this;
    }

    public UserAttributesBuilder withAcceptedTerms(URI acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
        return this;
    }

    public UserAttributesBuilder withUsers(List<UserDto> users) {
        this.users = users;
        return this;
    }

    public UserAttributesBuilder withCustomersForPerson(Set<CustomerDto> customersForPerson) {
        this.customersForPerson = customersForPerson;
        return this;
    }

    public UserAttributesBuilder withCurrentCustomer(CustomerDto currentCustomer) {
        this.currentCustomer = currentCustomer;
        return this;
    }

    public UserAttributesBuilder withAuthenticationDetails(AuthenticationDetails authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
        return this;
    }

    public UserAttributesBuilder withImpersonatedBy(String impersonatedBy) {
        this.impersonatedBy = impersonatedBy;
        return this;
    }

    public UserAttributesBuilder withCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
        return this;
    }

    public UserAttributesBuilder withAccessRightsPersistedFormat(List<String> accessRightsPersistedFormat) {
        this.accessRightsPersistedFormat = accessRightsPersistedFormat;
        return this;
    }

    public List<AttributeType> build() {
        var userAttributes = new ArrayList<AttributeType>();
        userAttributes.add(createAttribute(FIRST_NAME_CLAIM, person.getFirstname()));
        userAttributes.add(createAttribute(LAST_NAME_CLAIM, person.getSurname()));

        var userAcceptedTerms = currentTerms.equals(acceptedTerms);

        userAttributes.add(
            createAttribute(ACCESS_RIGHTS_CLAIM, String.join(ELEMENTS_DELIMITER, accessRightsPersistedFormat)));

        var rolesPerCustomerForPerson = rolesForCustomer(users, currentCustomer, userAcceptedTerms);
        userAttributes.add(createAttribute(ROLES_CLAIM, rolesPerCustomerForPerson));

        var allowedCustomersString = createAllowedCustomersString(userAcceptedTerms, customersForPerson,
                                                                  authenticationDetails.getFeideDomain());
        userAttributes.add(createAttribute(ALLOWED_CUSTOMERS_CLAIM, allowedCustomersString));

        if (nonNull(authenticationDetails.getFeideIdentifier())) {
            userAttributes.add(createAttribute(FEIDE_ID, authenticationDetails.getFeideIdentifier()));
        }

        userAttributes.add(createAttribute(PERSON_CRISTIN_ID_CLAIM, person.getId().toString()));
        userAttributes.add(createAttribute(IMPERSONATED_BY_CLAIM, isNull(impersonatedBy) ? "" : impersonatedBy));
        userAttributes.add(createAttribute(CURRENT_TERMS, currentTerms.toString()));
        userAttributes.add(
            createAttribute(CUSTOMER_ACCEPTED_TERMS, nonNull(acceptedTerms) ? acceptedTerms.toString() : ""));

        if (currentCustomer != null && currentUser != null) {
            userAttributes.add(createAttribute(CURRENT_CUSTOMER_CLAIM, currentCustomer.getId().toString()));
            userAttributes.add(createAttribute(TOP_ORG_CRISTIN_ID, currentCustomer.getCristinId().toString()));
            userAttributes.add(createAttribute(NVA_USERNAME_CLAIM, currentUser.getUsername()));
            userAttributes.add(createAttribute(PERSON_AFFILIATION_CLAIM, currentUser.getAffiliation().toString()));
        } else {
            userAttributes.add(createAttribute(CURRENT_CUSTOMER_CLAIM, EMPTY_CLAIM));
            userAttributes.add(createAttribute(TOP_ORG_CRISTIN_ID, EMPTY_CLAIM));
            userAttributes.add(createAttribute(NVA_USERNAME_CLAIM, EMPTY_CLAIM));
            userAttributes.add(createAttribute(PERSON_AFFILIATION_CLAIM, EMPTY_CLAIM));
        }

        var viewingScopeIncluded = getViewingScope(ViewingScope::getIncludedUnits, currentUser);
        userAttributes.add(
            createAttribute(VIEWING_SCOPE_INCLUDED_CLAIM, uriSetToCommaSeparatedString(viewingScopeIncluded)));

        var viewingScopeExcluded = getViewingScope(ViewingScope::getExcludedUnits, currentUser);
        userAttributes.add(
            createAttribute(VIEWING_SCOPE_EXCLUDED_CLAIM, uriSetToCommaSeparatedString(viewingScopeExcluded)));

        return userAttributes;
    }

    private String createAllowedCustomersString(boolean userAcceptedTerms, Collection<CustomerDto> allowedCustomers,
                                                String feideDomain) {
        var result = allowedCustomers
                         .stream()
                         .filter(isNotFeideRequestOrIsFeideRequestForCustomer(feideDomain))
                         .map(CustomerDto::getId)
                         .map(URI::toString)
                         .collect(Collectors.joining(ELEMENTS_DELIMITER));
        return StringUtils.isNotBlank(result) && userAcceptedTerms
                   ? result
                   : EMPTY_CLAIM;
    }

    private Predicate<CustomerDto> isNotFeideRequestOrIsFeideRequestForCustomer(String feideDomain) {
        return customer -> isNull(feideDomain)
                           || nonNull(customer.getFeideOrganizationDomain())
                              && customer.getFeideOrganizationDomain().equals(feideDomain);
    }

    Set<URI> getViewingScope(Function<ViewingScope, Set<URI>> unitExtractor, UserDto currentUser) {
        if (currentUser != null && currentUser.getViewingScope() != null) {
            return unitExtractor.apply(currentUser.getViewingScope());
        } else {
            return Collections.emptySet();
        }
    }

    private String uriSetToCommaSeparatedString(Set<URI> uris) {
        final String commaSeparatedString;

        if (uris.isEmpty()) {
            commaSeparatedString = EMPTY_CLAIM;
        } else {
            commaSeparatedString = uris.stream()
                                       .map(UriWrapper::fromUri)
                                       .map(UriWrapper::getLastPathElement)
                                       .collect(Collectors.joining(","));
        }
        return commaSeparatedString;
    }

    private String rolesForCustomer(List<UserDto> usersForPerson, CustomerDto customer, boolean userAcceptedTerms) {
        if (isNull(customer) || !userAcceptedTerms) {
            return EMPTY_STRING;
        }
        var rolesPerCustomerForPerson = usersForPerson.stream()
                                            .filter(user -> user.getInstitution().equals(customer.getId()))
                                            .map(UserDto::getRoles)
                                            .flatMap(Collection::stream)
                                            .map(RoleDto::getRoleName)
                                            .collect(Collectors.toUnmodifiableSet());

        return String.join(ELEMENTS_DELIMITER,
                           rolesPerCustomerForPerson.stream()
                               .map(RoleName::getValue)
                               .toList());
    }

    private AttributeType createAttribute(String name, String value) {
        return AttributeType.builder().name(name).value(value).build();
    }
}
