package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.core.paths.UriWrapper;

public class PeopleAndInstitutions {

    public static final boolean ACTIVE = true;
    private final CristinServerMock cristinServer;
    private final CustomerService customerService;

    public PeopleAndInstitutions(CustomerService customerService) {
        this.cristinServer = new CristinServerMock();
        this.customerService = customerService;
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var organization = cristinServer.randomOrgUri();
        var parentInstitution = cristinServer.randomOrgUri();
        cristinServer.addPerson(person, createActiveEmployment(organization, parentInstitution));
        registerInstitutionAsNvaCustomer(parentInstitution);

        return person;
    }

    public  URI getCristinId(NationalIdentityNumber person) {
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


    private PersonEmployment createActiveEmployment(URI organization, URI institution) {
        return PersonEmployment.builder().withChild(organization).withParent(institution).withActive(ACTIVE).build();
    }

}
