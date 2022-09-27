package no.unit.nva.cognito;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ImaginarySetup {
    
    private final MockPersonRegistry personRegistry;
    private final CustomerService customerService;
    private Map<NationalIdentityNumber, CustomerDto> customers;
    
    public ImaginarySetup(MockPersonRegistry personRegistry, CustomerService customerService) {
        this.personRegistry = personRegistry;
        this.customerService = customerService;
        this.customers = new ConcurrentHashMap<>();
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmployment() throws ConflictException, NotFoundException {
        var person = personRegistry.personWithExactlyOneActiveEmployment();
        var createdCustomer = newCustomerRequest(person).forEach(this::persistCustomer);
        customers.put(person, createdCustomer);
        return person;
    }
    
    public NationalIdentityNumber personWithExactlyOneInactiveEmployment() throws ConflictException, NotFoundException {
        var person = personRegistry.personWithExactlyOneInactiveEmployment();
        var createdCustomer = customerService.createCustomer(newCustomerRequest(person));
        customers.put(person, createdCustomer);
        return person;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmployment() {
        var person = personRegistry.personWithOneActiveAndOneInactiveEmployment();
        return person;
    }
    
    public CustomerDto fetchCustomerForPerson(NationalIdentityNumber nin) {
        return customers.get(nin);
    }
    
    private void persistCustomer(CustomerDto customer) {
        attempt(() -> customerService.createCustomer(customer)).orElseThrow();
    }
    
    private Stream<CustomerDto> newCustomerRequest(NationalIdentityNumber person) {
        return personRegistry.fetchTopLevelOrgsForPerson(person)
                   .stream()
                   .map(orgId -> CustomerDto.builder().withCristinId(orgId).build())
    }
}
