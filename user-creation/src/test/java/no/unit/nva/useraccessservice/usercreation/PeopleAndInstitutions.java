package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;

public class PeopleAndInstitutions {

    private final CristinServerMock cristinServer;

    public PeopleAndInstitutions() {
        this.cristinServer = new CristinServerMock();
    }

    public NationalIdentityNumber getPersonWithExactlyOneActiveAffiliation() {
        var person = new NationalIdentityNumber(randomString());
        var organization = randomUri();
        var institution = randomUri();
        cristinServer.addPerson(person, createActiveEmployment(organization, institution));
        return person;
    }

    private PersonEmployment createActiveEmployment(URI organization, URI institution) {
        return PersonEmployment.builder().withChild(organization).withParent(institution).withActive(true).build();
    }

    public CristinClient getCristinClient() {
        return null;
    }
}
