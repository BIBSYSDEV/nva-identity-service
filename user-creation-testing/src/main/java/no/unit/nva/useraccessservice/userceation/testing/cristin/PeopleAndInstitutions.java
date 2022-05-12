package no.unit.nva.useraccessservice.userceation.testing.cristin;

import static no.unit.nva.auth.AuthorizedBackendClient.prepareWithBearerToken;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;

public class PeopleAndInstitutions {

    public static final boolean ACTIVE = true;
    public static final boolean INACTIVE = false;
    public static final URI NOT_FILLED_IN = null;
    private final CristinServerMock cristinServer;
    private final CustomerService customerService;
    private final IdentityService identityService;

    public PeopleAndInstitutions(CustomerService customerService, IdentityService identityService) {
        this.cristinServer = new CristinServerMock();
        this.customerService = customerService;
        this.identityService = identityService;
    }

    public CristinClient createCristinClient() {
        var httpClient = prepareWithBearerToken(WiremockHttpClient.create(), randomString());
        return new CristinClient(cristinServer.getServerUri(), httpClient);
    }

    public void shutdown() {
        cristinServer.shutDown();
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() throws ApiGatewayException {
        var person = newPerson();
        var affiliation = createAffiliation(ACTIVE);
        cristinServer.addPerson(person, affiliation);
        return person;
    }

    public NationalIdentityNumber getPersonWithExactlyOneInactiveAffiliation()
        throws NotFoundException, ConflictException {
        var person = newPerson();
        var affiliation = createAffiliation(INACTIVE);
        cristinServer.addPerson(person, affiliation);
        return person;
    }

    public NationalIdentityNumber getPersonWithSomeActiveAndSomeInactiveAffiliations() {
        var person = newPerson();
        var activeAffiliations = createAffiliations(ACTIVE);
        var inactiveAffiliations = createAffiliations(INACTIVE);
        var allAffiliations = Stream.of(activeAffiliations, inactiveAffiliations)
            .flatMap(Function.identity())
            .toArray(PersonAffiliation[]::new);
        cristinServer.addPerson(person, allAffiliations);
        return person;
    }

    public NationalIdentityNumber getPersonAffiliatedWithNonNvaCustomerInstitution() {
        var person = newPerson();
        var nonNvaCustomerAffiliation = createNonNvaCustomerAffiliation();
        cristinServer.addPerson(person, nonNvaCustomerAffiliation);
        return person;
    }

    public NationalIdentityNumber getPersonThatIsNotRegisteredInPersonRegistry() {
        var person = newPerson();
        cristinServer.addNonExistentPerson(person);
        return person;
    }

    public URI getCristinId(NationalIdentityNumber person) {
        return cristinServer.getCristinId(person);
    }

    public URI getPersonAndInstitutionRegistryUri() {
        return cristinServer.getServerUri();
    }

    public List<URI> getParentIntitutionsWithActiveAffiliations(
        NationalIdentityNumber person) {
        return cristinServer.getActiveAffiliations(person)
            .stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinServer::getParentInstitution)
            .collect(Collectors.toList());
    }

    public UserDto createNvaUserForPerson(NationalIdentityNumber person) throws NotFoundException, ConflictException {
        var personAffiliation = getPersonAffiliations(person).stream().collect(SingletonCollector.collect());
        var personInstitution =
            getParentIntitutionsWithActiveAffiliations(person).stream().collect(SingletonCollector.collect());

        var userCustomer = customerService.getCustomerByCristinId(personInstitution);
        var user = createUserObject(person, personAffiliation, userCustomer);
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    public UserDto createLegacyNvaUserForPerson(NationalIdentityNumber person, String feideIdentifier)
        throws NotFoundException, ConflictException {
        var personInstitution =
            getParentIntitutionsWithActiveAffiliations(person).stream().collect(SingletonCollector.collect());
        var userCustomer = customerService.getCustomerByCristinId(personInstitution);
        var user = UserDto.newBuilder()
            .withInstitution(userCustomer.getId())
            .withUsername(feideIdentifier)
            .withFamilyName(randomString())
            .withGivenName(randomString())
            .withInstitutionCristinId(NOT_FILLED_IN)
            .withCristinId(NOT_FILLED_IN)
            .build();
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    public List<URI> getInstitutions(NationalIdentityNumber person) {
        return cristinServer.getActiveAffiliations(person).stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinServer::getParentInstitution)
            .collect(Collectors.toList());
    }

    public List<URI> getAffiliations(NationalIdentityNumber person) {
        return cristinServer.getPerson(person).getAffiliations().stream()
            .map(CristinAffiliation::getOrganizationUri)
            .collect(Collectors.toList());
    }

    private UserDto createUserObject(NationalIdentityNumber person, CristinAffiliation personAffiliation,
                                     CustomerDto userCustomer) {
        return UserDto.newBuilder()
            .withUsername(randomString())
            .withAffiliation(personAffiliation.getOrganizationUri())
            .withCristinId(getCristinId(person))
            .withInstitutionCristinId(userCustomer.getCristinId())
            .withInstitution(userCustomer.getId())
            .build();
    }

    private List<CristinAffiliation> getPersonAffiliations(NationalIdentityNumber person) {
        return cristinServer.getActiveAffiliations(person);
    }

    private Stream<PersonAffiliation> createAffiliations(boolean active) {
        return IntStream.range(0, 10).boxed()
            .map(attempt(ignored -> createAffiliation(active)))
            .map(Try::orElseThrow);
    }

    private NationalIdentityNumber newPerson() {
        return new NationalIdentityNumber(randomString());
    }

    private URI createOrganization() {
        return cristinServer.randomOrgUri();
    }

    private URI createNvaCustomerInstitution() throws NotFoundException, ConflictException {
        var parentInstitution = createOrganization();
        registerInstitutionAsNvaCustomer(parentInstitution);
        return parentInstitution;
    }

    private void registerInstitutionAsNvaCustomer(URI institution) throws NotFoundException, ConflictException {
        var customer = CustomerDto.builder().withCristinId(institution).build();
        customerService.createCustomer(customer);
    }

    private PersonAffiliation createAffiliation(boolean active) throws NotFoundException, ConflictException {
        return PersonAffiliation.builder()
            .withChild(createOrganization())
            .withParent(createNvaCustomerInstitution())
            .withActive(active).build();
    }

    private PersonAffiliation createNonNvaCustomerAffiliation() {
        return PersonAffiliation.builder()
            .withChild(createOrganization())
            .withParent(createOrganization())
            .withActive(ACTIVE)
            .build();
    }
}
