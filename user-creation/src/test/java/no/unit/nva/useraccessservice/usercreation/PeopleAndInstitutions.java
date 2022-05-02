package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import nva.commons.core.SingletonCollector;

public class PeopleAndInstitutions {

    public static final boolean ACTIVE = true;
    public static final boolean INACTIVE = false;
    private final CristinServerMock cristinServer;
    private final CustomerService customerService;
    private final IdentityService identityService;

    public PeopleAndInstitutions(CustomerService customerService, IdentityService identityService) {
        this.cristinServer = new CristinServerMock();
        this.customerService = customerService;
        this.identityService = identityService;
    }

    public void shutdown() {
        cristinServer.shutDown();
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {

        var person = newPerson();
        var affiliation = createAffiliation(ACTIVE);
        cristinServer.addPerson(person, affiliation);
        return person;
    }

    public NationalIdentityNumber getPersonWithExactlyOneInactiveAffiliation() {
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

    public URI getCristinId(NationalIdentityNumber person) {
        return cristinServer.getCristinId(person);
    }

    public URI getPersonAndInstitutionRegistryUri() {
        return cristinServer.getServerUri();
    }

    public List<URI> getParentIntituttionsWithActiveAffiliations(
        NationalIdentityNumber person) {
        return cristinServer.getActiveAffiliations(person)
            .stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinServer::getParentInstitution)
            .collect(Collectors.toList());
    }

    public UserDto createNvaUserForPerson(NationalIdentityNumber person) {
        var personInstitution =
            getParentIntituttionsWithActiveAffiliations(person).stream().collect(SingletonCollector.collect());
        var personAffiliation =
            cristinServer.getActiveAffiliations(person)
                .stream()
                .collect(SingletonCollector.collect());
        var userCustomer = customerService.getCustomerByCristinId(personInstitution);
        var user = UserDto.newBuilder()
                       .withUsername(randomString())
                       .withAffiliation(personAffiliation.getOrganizationUri())
                       .withCristinId(getCristinId(person))
                       .withInstitutionCristinId(userCustomer.getCristinId())
                       .withInstitution(userCustomer.getId())
                       .build();
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    private Stream<PersonAffiliation> createAffiliations(boolean active) {
        return IntStream.range(0, 10).boxed()
            .map(ignored -> createAffiliation(active));
    }

    private NationalIdentityNumber newPerson() {
        return new NationalIdentityNumber(randomString());
    }

    private URI createOrganization() {
        return cristinServer.randomOrgUri();
    }

    private URI createNvaCustomerInstitution() {
        var parentInstitution = createOrganization();
        registerInstitutionAsNvaCustomer(parentInstitution);
        return parentInstitution;
    }

    private void registerInstitutionAsNvaCustomer(URI institution) {
        var customer = CustomerDto.builder().withCristinId(institution).build();
        customerService.createCustomer(customer);
    }

    private PersonAffiliation createAffiliation(boolean active) {
        return PersonAffiliation.builder()
            .withChild(createOrganization())
            .withParent(createNvaCustomerInstitution())
            .withActive(active).build();
    }
}
