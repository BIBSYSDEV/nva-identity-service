package no.unit.nva.useraccessservice.userceation.testing.cristin;

import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import nva.commons.apigateway.exceptions.ConflictException;

public class AuthenticationScenarios {

    private final MockPersonRegistry personRegistry;
    private final CustomerService customerService;
    private final Map<String, List<CustomerDto>> personNinToCustomers;

    public AuthenticationScenarios(MockPersonRegistry personRegistry,
                                   CustomerService customerService,
                                   IdentityService identityService) throws InvalidInputException, ConflictException {
        this.personRegistry = personRegistry;
        this.customerService = customerService;
        this.personNinToCustomers = new ConcurrentHashMap<>();
        addCreatorRoleToIdentityService(identityService);
    }

    public String personWithTwoActiveEmploymentsInDifferentInstitutions() {
        var personNin = personRegistry.personWithTwoActiveEmploymentsInDifferentInstitutions();
        var withFeideDomain = true;
        registerTopOrganizationsAsCustomers(personNin, withFeideDomain);
        return personNin;
    }

    public String personWithExactlyOneActiveEmployment() {
        var personNin = personRegistry.personWithExactlyOneActiveEmployment();
        var withFeideDomain = true;
        registerTopOrganizationsAsCustomers(personNin, withFeideDomain);
        return personNin;
    }

    public String personWithTwoActiveEmploymentsInNonFeideAndFeideCustomers() {
        var personNin = personRegistry.personWithTwoActiveEmploymentsInDifferentInstitutions();
        registerTopOrganizationAsCustomerAlternatingFeideDomainSet(personNin);
        return personNin;
    }

    public String personWithExactlyOneActiveEmploymentInNonCustomer() {
        return personRegistry.personWithExactlyOneActiveEmployment();
    }

    public String personWithExactlyOneInactiveEmployment() {
        var personNin = personRegistry.personWithExactlyOneInactiveEmployment();
        var withFeideDomain = true;
        registerTopOrganizationsAsCustomers(personNin, withFeideDomain);
        return personNin;
    }

    public String personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions() {
        var personNin = personRegistry.personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions();
        var withFeideDomain = true;
        registerTopOrganizationsAsCustomers(personNin, withFeideDomain);
        return personNin;
    }

    public String personWithOneActiveAndOneInactiveEmploymentInSameInstitution() {
        var personNin = personRegistry.personWithOneActiveAndOneInactiveEmploymentInSameInstitution();
        var withFeideDomain = true;
        registerTopOrganizationsAsCustomers(personNin, withFeideDomain);
        return personNin;
    }

    public String personThatIsNotRegisteredInPersonRegistry() {
        return personRegistry.mockResponseForPersonNotFound();
    }

    public String failingPersonRegistryRequestBadGateway() {
        return personRegistry.mockResponseForBadGateway();
    }

    public String failingPersonRegistryRequestBadJson() {
        return personRegistry.mockResponseForIllegalJson();
    }

    private void addCreatorRoleToIdentityService(IdentityService identityService)
        throws InvalidInputException, ConflictException {
        var creatorRole = RoleDto.newBuilder().withRoleName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT)
                              .build();
        identityService.addRole(creatorRole);
    }

    public Set<URI> getCristinUriForInstitutionAffiliations(String nin, boolean active) {
        return personRegistry.getInstitutionUnitCristinUrisByState(nin, active);
    }

    public Set<URI> getCristinUriForInstitutionAffiliations(String nin) {
        return personRegistry.getInstitutionUnitCristinUris(nin);
    }

    public Set<URI> getCristinUriForUnitAffiliations(String nin) {
        return personRegistry.getUnitCristinUris(nin);
    }

    public URI getCristinIdForPerson(String nin) {
        return personRegistry.getCristinIdForPerson(nin);
    }

    public CristinPerson getPersonFromRegistry(String nin) {
        return personRegistry.getPerson(nin);
    }

    public List<CustomerDto> fetchCustomersForPerson(String nin) {
        return Optional.ofNullable(personNinToCustomers.get(nin)).orElse(Collections.emptyList());
    }

    private void registerTopOrganizationAsCustomerAlternatingFeideDomainSet(String personNin) {
        var customers = newCustomerRequests(personNin, true)
                            .collect(Collectors.toList());
        boolean withFeideDomain = true;
        for (var customer : customers) {
            if (!withFeideDomain) {
                customer.setFeideOrganizationDomain(null);
            }
            withFeideDomain = !withFeideDomain;
        }
        var persistedCustomers = customers.stream().map(this::persistCustomer).collect(Collectors.toList());
        personNinToCustomers.put(personNin, persistedCustomers);
    }

    private void registerTopOrganizationsAsCustomers(String personNin, boolean withFeideDomain) {
        var customers = newCustomerRequests(personNin, withFeideDomain)
                            .map(this::persistCustomer)
                            .collect(Collectors.toList());
        personNinToCustomers.put(personNin, customers);
    }

    private CustomerDto persistCustomer(CustomerDto customer) {
        return attempt(() -> customerService.createCustomer(customer)).orElseThrow();
    }

    private Stream<CustomerDto> newCustomerRequests(String personNin, boolean withFeideDomain) {
        return personRegistry.getInstitutionUnitCristinUrisByState(personNin, true)
                   .stream()
                   .distinct()
                   .map(orgId -> buildCustomerDto(withFeideDomain, orgId));
    }

    private CustomerDto buildCustomerDto(boolean withFeideDomain, URI orgId) {
        var builder = CustomerDto.builder().withCristinId(orgId);
        if (withFeideDomain) {
            builder.withFeideOrganizationDomain(randomString());
        }
        return builder.build();
    }

    public List<UserDto> createUsersForAllActiveAffiliations(String nin, IdentityService identityService) {
        var personFromRegistry = getPersonFromRegistry(nin);

        return personFromRegistry.getAffiliations().stream()
                   .filter(CristinAffiliation::isActive)
                   .map(affiliation -> createUserForAffiliation(nin, affiliation, identityService))
                   .collect(Collectors.toList());
    }

    public List<UserDto> createLegacyUsersForAllActiveAffiliations(String nin,
                                                                   String feideIdentifier,
                                                                   IdentityService identityService) {
        var personFromRegistry = getPersonFromRegistry(nin);

        return personFromRegistry.getAffiliations().stream()
                   .filter(CristinAffiliation::isActive)
                   .map(affiliation -> createLegacyUserForAffiliation(feideIdentifier, affiliation,
                                                                      identityService))
                   .collect(Collectors.toList());
    }

    private UserDto createUserForAffiliation(String nin,
                                             CristinAffiliation affiliation,
                                             IdentityService identityService) {

        var institutionCristinId = personRegistry.getCristinIdForInstitution(affiliation.getInstitution().getId());
        var unitCristinId = personRegistry.getCristinIdForUnit(affiliation.getUnit().getId());
        var customerId = attempt(() -> customerService.getCustomerByCristinId(institutionCristinId)).map(
            CustomerDto::getId).orElseThrow();
        var user = UserDto.newBuilder()
                       .withAffiliation(unitCristinId)
                       .withCristinId(personRegistry.getCristinIdForPerson(nin))
                       .withUsername(randomString())
                       .withFamilyName(randomString())
                       .withGivenName(randomString())
                       .withFeideIdentifier(randomString())
                       .withInstitution(customerId)
                       .withInstitutionCristinId(institutionCristinId)
                       .build();
        return attempt(() -> identityService.addUser(user)).orElseThrow();
    }

    private UserDto createLegacyUserForAffiliation(String feideIdentifier,
                                                   CristinAffiliation affiliation,
                                                   IdentityService identityService) {

        var institutionCristinId = personRegistry.getCristinIdForInstitution(affiliation.getInstitution().getId());
        var unitCristinId = personRegistry.getCristinIdForUnit(affiliation.getUnit().getId());
        var customerId = attempt(() -> customerService.getCustomerByCristinId(institutionCristinId)).map(
            CustomerDto::getId).orElseThrow();
        var user = UserDto.newBuilder()
                       .withAffiliation(unitCristinId)
                       .withCristinId(null)
                       .withUsername(Objects.nonNull(feideIdentifier) ? feideIdentifier : randomString())
                       .withFamilyName(randomString())
                       .withGivenName(randomString())
                       .withFeideIdentifier(Objects.nonNull(feideIdentifier) ? feideIdentifier : randomString())
                       .withInstitution(customerId)
                       .withInstitutionCristinId(null)
                       .build();
        return attempt(() -> identityService.addUser(user)).orElseThrow();
    }
}
