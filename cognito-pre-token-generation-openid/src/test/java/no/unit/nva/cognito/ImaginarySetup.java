package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cognito.MockPersonRegistry.EmploymentInformation;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.ConflictException;

public class ImaginarySetup {
    
    private final MockPersonRegistry personRegistry;
    private final CustomerService customerService;
    private final Map<NationalIdentityNumber, List<CustomerDto>> personToCustomers;
    
    public ImaginarySetup(MockPersonRegistry personRegistry,
                          CustomerService customerService,
                          IdentityService identityService) throws InvalidInputException, ConflictException {
        this.personRegistry = personRegistry;
        this.customerService = customerService;
        this.personToCustomers = new ConcurrentHashMap<>();
        addCreatorRoleToIdentityService(identityService);
    }
    
    public NationalIdentityNumber personWithTwoActiveEmployments() {
        var person = personRegistry.personWithTwoActiveEmploymentsInDifferentTopLevelOrgs();
        registerTopOrganizationsAsCustomers(person);
        return person;
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmployment() {
        var person = personRegistry.personWithExactlyOneActiveEmployment();
        registerTopOrganizationsAsCustomers(person);
        return person;
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmploymentInNonRegisteredTopLevelOrg() {
        return personRegistry.personWithExactlyOneActiveEmployment();
    }
    
    public NationalIdentityNumber personWithExactlyOneInactiveEmployment() {
        var person = personRegistry.personWithExactlyOneInactiveEmployment();
        registerTopOrganizationsAsCustomers(person);
        return person;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmploymentInDifferentTopLevelOrgs() {
        var person = personRegistry.personWithOneActiveAndOneInactiveEmploymentInDifferentOrgs();
        registerTopOrganizationsAsCustomers(person);
        return person;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmploymentInSameTopLevelOrg() {
        var person = personRegistry.personWithOneActiveAndOneInactiveEmploymentInSameOrg();
        registerTopOrganizationsAsCustomers(person);
        return person;
    }
    
    private void addCreatorRoleToIdentityService(IdentityService identityService)
        throws InvalidInputException, ConflictException {
        var creatorRole = RoleDto.newBuilder().withRoleName("Creator").build();
        identityService.addRole(creatorRole);
    }
    
    public List<EmploymentInformation> fetchTopOrgEmploymentInformation(NationalIdentityNumber person) {
        return personRegistry.fetchTopOrgEmploymentInformation(person);
    }
    
    public CristinPersonResponse getPerson(NationalIdentityNumber person) {
        return personRegistry.getPerson(person);
    }
    
    public URI getTopLevelOrgForNonTopLevelOrg(URI organizationUri) {
        return personRegistry.getTopLevelOrgForNonTopLevelOrg(organizationUri);
    }
    
    public List<CustomerDto> fetchCustomersForPerson(NationalIdentityNumber nin) {
        return Optional.ofNullable(personToCustomers.get(nin)).orElse(Collections.emptyList());
    }
    
   
    
    private void registerTopOrganizationsAsCustomers(NationalIdentityNumber person) {
        var customers = newCustomerRequests(person)
                            .map(this::persistCustomer).collect(Collectors.toList());
        personToCustomers.put(person, customers);
    }
    
    private CustomerDto persistCustomer(CustomerDto customer) {
        return attempt(() -> customerService.createCustomer(customer)).orElseThrow();
    }
    
    private Stream<CustomerDto> newCustomerRequests(NationalIdentityNumber person) {
        return personRegistry.fetchTopOrgEmploymentInformation(person)
                   .stream()
                   .map(EmploymentInformation::getTopLevelOrg)
                   .distinct()
                   .map(orgId -> CustomerDto.builder().withCristinId(orgId)
                                     .withFeideOrganizationDomain(randomString())
                                     .build());
    }
}
