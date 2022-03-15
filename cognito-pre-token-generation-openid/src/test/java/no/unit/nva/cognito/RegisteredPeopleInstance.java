package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;

public class RegisteredPeopleInstance {

    private final CustomerService customerService;
    private final CristinProxyMock cristinProxy;
    private final Set<NationalIdentityNumber> people;

    public RegisteredPeopleInstance(WireMockServer httpServer,
                                    DataportenMock dataportenMock,
                                    CustomerService customerService) {
        this.customerService = customerService;
        this.cristinProxy = new CristinProxyMock(httpServer, dataportenMock);
        this.people = randomPeople();
        initialize();
    }

    public void initialize() {
        cristinProxy.initialize(people);
        populateCustomersFoundInCristinProxy();
    }

    public static Set<NationalIdentityNumber> randomPeople() {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> new NationalIdentityNumber(randomString()))
            .collect(Collectors.toSet());
    }

    public Set<URI> getTopLevelOrgsForPerson(NationalIdentityNumber nin, boolean includeInactive) {
        return cristinProxy.getCristinPersonRegistry(nin).getAffiliations()
            .stream()
            .filter(org -> org.isActive() || includeInactive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getTopLevelOrgForBottomLevelOrg)
            .collect(Collectors.toSet());
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

    public NationalIdentityNumber getPersonWithActiveAndInactiveAffiliations() {
        return cristinProxy.getPersonWithActiveAndInactiveAffiliations();
    }

    public Set<URI> getTopLevelAffiliationsForUser(NationalIdentityNumber nin, boolean active) {
        return cristinProxy.getCristinPersonRegistry(nin).getAffiliations()
            .stream()
//            .filter(aff -> aff.isActive() == active)
            .map(aff -> aff.getOrganizationUri())
            .map(uri -> cristinProxy.getTopLevelOrgForBottomLevelOrg(uri))
            .collect(Collectors.toSet());
    }

    private void populateCustomersFoundInCristinProxy() {
        var nvaCustomers = cristinProxy.getTopLevelOrgUris().stream()
            .map(this::createNvaCustomer)
            .collect(Collectors.toList());
        assertThat(nvaCustomers, is(not(empty())));
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
}
