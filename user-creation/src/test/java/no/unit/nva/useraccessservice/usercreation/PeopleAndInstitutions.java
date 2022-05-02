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

    public void shutdown() {
        cristinServer.shutDown();
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var organization = cristinServer.randomOrgUri();
        var parentInstitution = cristinServer.randomOrgUri();
        var activeEmployment = createEmployment(organization, parentInstitution, ACTIVE);
        cristinServer.addPerson(person, activeEmployment);

        registerInstitutionAsNvaCustomer(parentInstitution);

        return person;
    }

    public NationalIdentityNumber getPersonWithExactlyOneInActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var organization = cristinServer.randomOrgUri();
        var parentInstitution = cristinServer.randomOrgUri();
        cristinServer.addPerson(person, createEmployment(organization, parentInstitution, INACTIVE));
        registerInstitutionAsNvaCustomer(parentInstitution);

        return person;
    }

    public URI getCristinId(NationalIdentityNumber person) {
        return cristinServer.getCristinId(person);
    }

    public URI getPersonAndInstitutionRegistryUri() {
        return cristinServer.getServerUri();
    }

    private void registerInstitutionAsNvaCustomer(URI institution) {
        var customer = CustomerDto.builder().withCristinId(institution).build();
        customerService.createCustomer(customer);
    }

    private PersonEmployment createEmployment(URI organization, URI institution, boolean active) {
        return PersonEmployment.builder().withChild(organization).withParent(institution).withActive(active).build();
    }
}
