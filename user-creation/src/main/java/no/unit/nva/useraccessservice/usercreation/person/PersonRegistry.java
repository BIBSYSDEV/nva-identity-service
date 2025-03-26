package no.unit.nva.useraccessservice.usercreation.person;

import java.util.Optional;

public interface PersonRegistry {

    Optional<Person> fetchPersonByNin(NationalIdentityNumber nin);

    Optional<Person> fetchPersonByIdentifier(String identifier);
}
