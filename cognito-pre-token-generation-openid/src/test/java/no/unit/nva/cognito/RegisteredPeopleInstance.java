package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;

public class RegisteredPeopleInstance {

    private final CustomerService customerService;
    private final CristinProxyMock cristinProxy;

    public RegisteredPeopleInstance(CristinProxyMock cristinProxy,
                                    CustomerService customerService) {
        this.customerService = customerService;
        this.cristinProxy = cristinProxy;
        populateCustomerService();
        cristinProxy.setup();
        populateCustomersFoundInCristinProxy();
    }

    private void populateCustomerService() {
        IntStream.range(0,5+randomInteger(10)).boxed()
            .map(ignored->randomCustomer(cristinProxy.randomOrgUri()))
            .forEach(customerService::createCustomer);
    }

    public List<URI> getTopLevelOrgsForPerson(NationalIdentityNumber nin, boolean includeInactive) {
        return cristinProxy.getTopLevelOrgsForPerson(nin)
            .stream()
            .filter(org->org.isActiveAffiliation() || includeInactive)
            .map(TopLevelOrg::getTopLevelOrg)
            .collect(Collectors.toList());
    }

    public CristinIdentifier getCristinPersonIdentifier(NationalIdentityNumber nin) {
        return cristinProxy.getCristinPersonIdentifier(nin);
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {
        return cristinProxy.getPersonWithOneActiveAffiliationAndNoInactiveAffiliations();
    }

    public NationalIdentityNumber getPersonWithOnlyInactiveAffiliations() {
        return cristinProxy.getPersonWithOnlyInactiveAffiliations();
    }

    public CristinPersonResponse getCristinPersonRecord(NationalIdentityNumber personLoggingIn) {
        return cristinProxy.getCristinPersonRegistry(personLoggingIn);
    }

    public List<URI> getActiveAffiliations(NationalIdentityNumber personLoggingIn) {
        return cristinProxy.getTopLevelOrgsForPerson(personLoggingIn)
            .stream()
            .filter(TopLevelOrg::isActiveAffiliation)
            .map(TopLevelOrg::getTopLevelOrg)
            .collect(Collectors.toList());
    }

    public NationalIdentityNumber getPersonWithActiveAndInactiveAffiliations() {
        return cristinProxy.getPersonWithActiveAndInactiveAffiliations();
    }

    private void populateCustomersFoundInCristinProxy() {
        cristinProxy.getTopLevelOrgs().map(TopLevelOrg::getTopLevelOrg).forEach(this::createNvaCustomer);
    }

    private CustomerDto createNvaCustomer(URI topLevelOrg) {
        var customer = randomCustomer(topLevelOrg);
        var savedCustomer = customerService.createCustomer(customer);
        return customerService.getCustomer(savedCustomer.getIdentifier());
    }

    private CustomerDto randomCustomer(URI topLevelOrg) {
        return CustomerDto.builder()
            .withCname(randomString())
            .withCristinId(topLevelOrg.toString())
            .withArchiveName(randomString())
            .withName(randomString())
            .withShortName(randomString())
            .build();
    }

    public static Set<NationalIdentityNumber> randomPeople(int numberOfPeople) {
        return IntStream.range(0, numberOfPeople).boxed()
            .map(ignored -> new NationalIdentityNumber(randomString()))
            .collect(Collectors.toSet());
    }
}
