package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;

public class PeopleAndInstitutions {

    public static final boolean ACTIVE = true;
    public static final boolean INACTIVE = false;
    private final CristinServerMock cristinServer;
    private final CustomerService customerService;

    public PeopleAndInstitutions(CustomerService customerService) {
        this.cristinServer = new CristinServerMock();
        this.customerService = customerService;
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var affiliation = createAffiliation(createOrganization(), createNvaCustomerInstitution(), ACTIVE);
        cristinServer.addPerson(person, affiliation);
        return person;
    }

    public NationalIdentityNumber getPersonWithExactlyOneInActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var affiliation = createAffiliation(createOrganization(), createNvaCustomerInstitution(), INACTIVE);
        cristinServer.addPerson(person, affiliation);
        return person;
    }

    private URI createOrganization() {
        return cristinServer.randomOrgUri();
    }

    private URI createNvaCustomerInstitution() {
        var parentInstitution = createOrganization();
        registerInstitutionAsNvaCustomer(parentInstitution);
        return parentInstitution;
    }

    public URI getCristinId(NationalIdentityNumber person) {
        return cristinServer.getCristinId(person);
    }

    private void registerInstitutionAsNvaCustomer(URI institution) {
        var customer = CustomerDto.builder().withCristinId(institution).build();
        customerService.createCustomer(customer);
    }

    public void shutdown() {
        cristinServer.shutDown();
    }

    public URI getPersonAndInstitutionRegistryUri() {
        return cristinServer.getServerUri();
    }

    private PersonAffiliation createAffiliation(URI organization, URI institution, boolean active) {
        return PersonAffiliation.builder().withChild(organization).withParent(institution).withActive(active).build();
    }
}
