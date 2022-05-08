package no.unit.nva.useraccessservice.usercreation;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

public class UserEntriesCreatorForPerson {

    public static final RoleDto ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION =
        RoleDto.newBuilder().withRoleName("Creator").build();
    private static final String AT = "@";
    private final CustomerService customerService;
    private final CristinClient cristinClient;
    private final IdentityService identityService;

    public UserEntriesCreatorForPerson(CustomerService customerService,
                                       CristinClient cristinClient,
                                       IdentityService identityService) {
        this.customerService = customerService;
        this.cristinClient = cristinClient;
        this.identityService = identityService;
    }

    public List<UserDto> createUsers(PersonInformation authenticationInfo) {
        createUserRole();
        return createOrFetchUserEntriesForPerson(authenticationInfo, keepAll());
    }

    public List<UserDto> createUser(PersonInformation authenticationInfo, URI selectedCustomer) {
        createUserRole();
        return createOrFetchUserEntriesForPerson(authenticationInfo,
                                                 customerDto -> isSelectedCustomer(customerDto, selectedCustomer));
    }

    private boolean isSelectedCustomer(CustomerDto customerDto, URI selectedCustomerId) {
        return customerDto.getId().equals(selectedCustomerId) || customerDto.getCristinId().equals(selectedCustomerId);
    }

    public PersonInformation collectPersonInformation(NationalIdentityNumber nationalIdentityNumber) {
        return collectPersonInformation(nationalIdentityNumber, null, null);
    }

    public PersonInformation collectPersonInformation(NationalIdentityNumber nationalIdentityNumber,
                                                      String personFeideIdentifier,
                                                      String orgFeideDomain) {
        var personInformation = new PersonInformationImpl(personFeideIdentifier, orgFeideDomain);
        var cristinResponse = fetchPersonInformationFromCristin(nationalIdentityNumber);
        personInformation.setCristinPersonResponse(cristinResponse);

        var affiliationInformation = fetchParentInstitutionsForPersonAffiliations(personInformation);
        personInformation.setPersonAffiliations(affiliationInformation);

        var activeCustomers = fetchCustomersForActiveAffiliations(personInformation);
        personInformation.setActiveCustomers(activeCustomers);

        return personInformation;
    }

    private Predicate<CustomerDto> keepAll() {
        return customerDto -> true;
    }

    private List<UserDto> createOrFetchUserEntriesForPerson(PersonInformation personInformation,
                                                            Predicate<CustomerDto> filterActiveCustomers) {

        var customers = personInformation.getActiveCustomers();
        return customers.stream()
            .filter(filterActiveCustomers::test)
            .map(customer -> createNewUserObject(customer, personInformation))
            .map(user -> getExistingUserOrCreateNew(user, personInformation))
            .collect(Collectors.toList());
    }

    private void createUserRole() {
        try {
            identityService.addRole(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION);
        } catch (Exception ignored) {
            //Do nothing if role exists.
        }
    }

    private Set<CustomerDto> fetchCustomersForActiveAffiliations(PersonInformation personInformation) {

        return personInformation.getPersonAffiliations()
            .stream()
            .map(PersonAffiliation::getParentInstitution)
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .collect(Collectors.toSet());
    }

    private List<PersonAffiliation> fetchParentInstitutionsForPersonAffiliations(
        PersonInformation personInformation) {
        return personInformation.getCristinPersonResponse().getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::fetchParentInstitutionCristinId)
            .collect(Collectors.toList());
    }

    private PersonAffiliation fetchParentInstitutionCristinId(URI mostSpecificAffiliation) {
        return attempt(() -> cristinClient.fetchTopLevelOrgUri(mostSpecificAffiliation))
            .map(parentInstitution -> PersonAffiliation.create(mostSpecificAffiliation, parentInstitution))
            .orElseThrow();
    }

    private CristinPersonResponse fetchPersonInformationFromCristin(NationalIdentityNumber nin) {
        return attempt(() -> cristinClient.sendRequestToCristin(nin)).orElseThrow();
    }

    private UserDto createNewUserObject(CustomerDto customer, PersonInformation personInformation) {

        var cristinResponse = personInformation.getCristinPersonResponse();
        var affiliation = personInformation.getOrganizationAffiliation(customer.getCristinId());
        var feideIdentifier = personInformation.getPersonFeideIdentifier();
        var user = UserDto.newBuilder()
            .withUsername(createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(cristinResponse, customer))
            .withRoles(Collections.singletonList(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
            .withFeideIdentifier(feideIdentifier)
            .withInstitution(customer.getId())
            .withGivenName(cristinResponse.extractFirstName())
            .withFamilyName(cristinResponse.extractLastName())
            .withCristinId(cristinResponse.getCristinId())
            .withCristinId(cristinResponse.getCristinId())
            .withInstitutionCristinId(customer.getCristinId())
            .withAffiliation(affiliation);

        return user.build();
    }

    // Create a username that will allow the user to access their resources even if the identity service stack
    // gets totally destroyed.
    private String createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(
        CristinPersonResponse cristinResponse,
        CustomerDto customer) {
        var personIdentifier = cristinResponse.getPersonsCristinIdentifier().getValue();
        var customerIdentifier = UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
        return personIdentifier + AT + customerIdentifier;
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, PersonInformation personInformation) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user, personInformation))
            .or(() -> fetchLegacyUserWithFeideIdentifier(user, personInformation))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentifier(UserDto userWithUpdatedInformation,
                                                       PersonInformation personInformation) throws NotFoundException {
        var queryObject =
            UserDto.newBuilder().withUsername(personInformation.getPersonFeideIdentifier()).build();
        var savedUser = identityService.getUser(queryObject);
        var affiliation =
            personInformation.getOrganizationAffiliation(userWithUpdatedInformation.getInstitutionCristinId());
        var updatedUser = savedUser.copy()
            .withFeideIdentifier(userWithUpdatedInformation.getFeideIdentifier())
            .withCristinId(userWithUpdatedInformation.getCristinId())
            .withInstitutionCristinId(userWithUpdatedInformation.getInstitutionCristinId())
            .withAffiliation(affiliation)
            .build();
        identityService.updateUser(updatedUser);
        return updatedUser;
    }

    private UserDto fetchUserBasedOnCristinIdentifiers(UserDto user, PersonInformation personInformation)
        throws NotFoundException {
        var existingUser =
            identityService.getUserByPersonCristinIdAndCustomerCristinId(user.getCristinId(),
                                                                         user.getInstitutionCristinId());
        return updateUserAffiliation(existingUser, personInformation);
    }

    private UserDto updateUserAffiliation(UserDto existingUser,
                                          PersonInformation personInformation) throws NotFoundException {
        var affiliation = personInformation.getOrganizationAffiliation(existingUser.getInstitutionCristinId());
        var updatedUser = existingUser.copy().withAffiliation(affiliation).build();
        identityService.updateUser(updatedUser);
        return updatedUser;
    }

    private UserDto addUser(UserDto user) throws ConflictException {
        identityService.addUser(user);
        return user;
    }
}
