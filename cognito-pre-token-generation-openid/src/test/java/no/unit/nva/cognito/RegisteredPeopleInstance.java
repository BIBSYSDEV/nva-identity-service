package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.RoleDto;

public class RegisteredPeopleInstance {

    public static final boolean ACTIVE = true;
    private final CustomerService customerService;
    private final CristinProxyMock cristinProxy;
    private final Set<NationalIdentityNumber> people;
    private List<RoleDto> availableNvaRoles;
    private IdentityService identityService;

    public RegisteredPeopleInstance(WireMockServer httpServer,
                                    DataportenMock dataportenMock,
                                    CustomerService customerService,
                                    IdentityService identityService) {
        this.customerService = customerService;
        this.cristinProxy = new CristinProxyMock(httpServer, dataportenMock);
        this.people = randomPeople();
        this.identityService = identityService;
        initialize();
    }

    public static Set<NationalIdentityNumber> randomPeople() {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> new NationalIdentityNumber(randomString()))
            .collect(Collectors.toSet());
    }

    public List<RoleDto> getAvailableNvaRoles() {
        return availableNvaRoles;
    }

    public CristinProxyMock getCristinProxy() {
        return cristinProxy;
    }

    public void initialize() {
        cristinProxy.initialize(people);
        populateCustomersFoundInCristinProxy();
        availableNvaRoles = createNvaRoles();
    }

    public Set<URI> getTopLevelOrgsForPerson(NationalIdentityNumber nin, boolean includeInactive) {
        return cristinProxy.getCristinPersonRecord(nin).getAffiliations()
            .stream()
            .filter(org -> org.isActive() || includeInactive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getTopLevelOrgForBottomLevelOrg)
            .collect(Collectors.toSet());
    }

    public CristinPersonResponse getCristinPersonRecord(NationalIdentityNumber nin) {
        return cristinProxy.getCristinPersonRecord(nin);
    }
    public URI getCristinPersonId(NationalIdentityNumber nin) {
        return cristinProxy.getCristinPersonRecord(nin).getCristinId();
    }

    public CristinIdentifier getCristinPersonIdentifier(NationalIdentityNumber nin) {
        return CristinIdentifier.fromCristinId(getCristinPersonId(nin));
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
        return cristinProxy.getCristinPersonRecord(nin).getAffiliations()
            .stream()
            .filter(aff -> aff.isActive() == active)
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getTopLevelOrgForBottomLevelOrg)
            .collect(Collectors.toSet());
    }

    public Set<URI> getAllTopLevelAffiliationsForUser(NationalIdentityNumber nin) {
        return cristinProxy.getCristinPersonRecord(nin).getAffiliations()
            .stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getTopLevelOrgForBottomLevelOrg)
            .collect(Collectors.toSet());
    }

    public Stream<CustomerDto> getCustomersWithActiveAffiliations(NationalIdentityNumber personsNin) {
        return getTopLevelAffiliationsForUser(personsNin, ACTIVE)
            .stream()
            .map(uri -> customerService.getCustomerByCristinId(uri.toString()));
    }



    private List<RoleDto> createNvaRoles() {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> NvaDataGenerator.createRole())
            .peek(role -> identityService.addRole(role))
            .map(role -> identityService.getRole(role))
            .collect(Collectors.toList());
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
