package no.unit.nva.cognito;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinIdentifier;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;

public class RegisteredPeopleInstance {

    public static final boolean ACTIVE = true;
    private final CustomerService customerService;
    private final CristinProxyMock cristinProxy;
    private final Set<NationalIdentityNumber> people;
    private final IdentityService identityService;
    private List<RoleDto> availableNvaRoles;

    public RegisteredPeopleInstance(WireMockServer httpServer,
                                    NvaAuthServerMock dataportenMock,
                                    CustomerService customerService,
                                    IdentityService identityService) {
        this.customerService = customerService;
        this.cristinProxy = new CristinProxyMock(httpServer, dataportenMock);
        this.people = randomPeople();
        this.identityService = identityService;
        initializeInstance();
    }

    public static Set<NationalIdentityNumber> randomPeople() {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> new NationalIdentityNumber(randomString()))
            .collect(Collectors.toSet());
    }

    public List<RoleDto> getAvailableNvaRoles() {
        return availableNvaRoles;
    }

    public void initializeInstance() {
        cristinHasSomeOrganizationsAndPeopleWorkingInOrganizations();
        nvaHasRegisteredSomeOfCristinsOrganizationsAsCustomers();
        nvaHasDefinedRolesForTheNvaUsers();
    }

    public String getFeideIdentifierForPerson() {
        return randomString();
    }

    public String getSomeFeideOrgIdentifierForPerson(NationalIdentityNumber nin) {
        var domains = getTopLevelAffiliationsForUser(nin, ACTIVE).stream()
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .map(CustomerDto::getFeideOrganizationDomain)
            .collect(Collectors.toList());
        return domains.isEmpty()
                   ? randomString()
                   : randomElement(domains.toArray(String[]::new));
    }

    public NationalIdentityNumber personWithActiveAffiliationThatIsNotCustomer() {
        return cristinProxy.getPersonWithActiveAffiliationThatIsNotCustomer();
    }

    public Set<URI> getTopLevelOrgsForPerson(NationalIdentityNumber nin, boolean includeInactive) {
        return cristinProxy.getCristinPersonRecord(nin).stream()
            .map(CristinPersonResponse::getAffiliations)
            .flatMap(Collection::stream)
            .filter(org -> org.isActive() || includeInactive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getParentInstitutionForOrganization)
            .collect(Collectors.toSet());
    }

    public NationalIdentityNumber personThatIsNotRegisteredInPersonRegistry() {
        return cristinProxy.getPersonThatInNotRegisteredInPersonRegistry();
    }

    public URI getCristinPersonId(NationalIdentityNumber nin) {
        return cristinProxy.getCristinPersonRecord(nin).stream()
            .map(CristinPersonResponse::getCristinId)
            .collect(SingletonCollector.collect());
    }

    public CristinIdentifier getCristinPersonIdentifier(NationalIdentityNumber nin) {
        return CristinIdentifier.fromCristinId(getCristinPersonId(nin));
    }

    public NationalIdentityNumber personWithExactlyOneActiveAffiliation() {
        return cristinProxy.getPersonWithOneActiveAffiliationAndNoInactiveAffiliations();
    }

    public NationalIdentityNumber personWithOnlyInactiveAffiliations() {
        return cristinProxy.getPersonWithOnlyInactiveAffiliations();
    }

    public NationalIdentityNumber personWithActiveAndInactiveAffiliations() {
        return cristinProxy.getPersonWithActiveAndInactiveAffiliations();
    }

    public NationalIdentityNumber personWithManyActiveAffiliations() {
        return cristinProxy.getPersonWithManyActiveAffiliations();
    }

    public NationalIdentityNumber personWithTwoActiveAffiliationsThatShareTheSameFeideDomain() {
        return cristinProxy.getPersonWithTwoActiveAffiliationThatShareTheSameFeideDomain();
    }

    public Set<URI> getTopLevelAffiliationsForUser(NationalIdentityNumber nin, boolean active) {
        return cristinProxy.getCristinPersonRecord(nin)
            .stream()
            .map(CristinPersonResponse::getAffiliations)
            .flatMap(Collection::stream)
            .filter(aff -> aff.isActive() == active)
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinProxy::getParentInstitutionForOrganization)
            .collect(Collectors.toSet());
    }

    public Stream<CustomerDto> getCustomersWithActiveAffiliations(NationalIdentityNumber personsNin) {
        return getTopLevelAffiliationsForUser(personsNin, ACTIVE)
            .stream()
            .map(attempt(customerService::getCustomerByCristinId))
            .map(Try::orElseThrow);
    }

    public List<URI> getOrganizations(NationalIdentityNumber person) {
        return cristinProxy.getCristinPersonRecord(person).stream()
            .map(CristinPersonResponse::getAffiliations)
            .flatMap(Collection::stream)
            .map(CristinAffiliation::getOrganizationUri)
            .collect(Collectors.toList());
    }

    public CristinProxyMock getCristinProxy() {
        return this.cristinProxy;
    }


    private void nvaHasDefinedRolesForTheNvaUsers() {
        availableNvaRoles = insertSomeRolesInNva();
    }

    private void cristinHasSomeOrganizationsAndPeopleWorkingInOrganizations() {
        cristinProxy.initialize(people);
    }

    private List<RoleDto> insertSomeRolesInNva() {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> NvaDataGenerator.createRole())
            .peek(this::addRole)
            .map(attempt(identityService::getRole))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private void addRole(RoleDto roleDto) {
        try {
            identityService.addRole(roleDto);
        } catch (ConflictException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    private void nvaHasRegisteredSomeOfCristinsOrganizationsAsCustomers() {
        var nvaCustomers = cristinProxy.getParentInstitutionsThatAreNvaCustomers().stream()
            .map(this::createNvaCustomer)
            .collect(Collectors.toList());
        assertThat(nvaCustomers, is(not(empty())));
    }

    private CustomerDto createNvaCustomer(URI topLevelOrg) {
        var customer = randomCustomer(topLevelOrg);
        var savedCustomer = attempt(() -> customerService.createCustomer(customer)).orElseThrow();
        return attempt(() -> customerService.getCustomer(savedCustomer.getIdentifier())).orElseThrow();
    }

    private CustomerDto randomCustomer(URI topLevelOrg) {
        return CustomerDto.builder()
            .withCname(randomString())
            .withCristinId(topLevelOrg)
            .withArchiveName(randomString())
            .withName(randomString())
            .withShortName(randomString())
            .withFeideOrganizationDomain(randomString())
            .build();
    }
}
