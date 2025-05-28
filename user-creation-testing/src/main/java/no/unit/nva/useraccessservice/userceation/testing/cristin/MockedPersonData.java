package no.unit.nva.useraccessservice.userceation.testing.cristin;

import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;

public record MockedPersonData(String nin, String cristinIdentifier) {

    public CristinPerson getCristinPerson() {
        return new CristinPerson(
            cristinIdentifier,
            "Test",
            "User",
            null,
            nin
        );
    }
}
