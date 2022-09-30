package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cognito.MockPersonRegistry.EmploymentInformation;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ImaginarySetup {
    
    private final MockPersonRegistry personRegistry;
    private final CustomerService customerService;
    private final Map<NationalIdentityNumber, List<CustomerDto>> personToCustomers;
    
    public ImaginarySetup(MockPersonRegistry personRegistry, CustomerService customerService) {
        this.personRegistry = personRegistry;
        this.customerService = customerService;
        this.personToCustomers = new ConcurrentHashMap<>();
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmployment() throws ConflictException, NotFoundException {
        var person = personRegistry.personWithExactlyOneActiveEmployment();
        registerTopOrganizationsAsCustomers(person);
        return person;
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
        return personToCustomers.get(nin);
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
                   .map(orgId -> CustomerDto.builder().withCristinId(orgId).build());
    }
}
