package no.unit.nva.useraccessservice.usercreation.person;

import java.util.Optional;

public interface PersonRegistry {

  Optional<Person> fetchPersonByNin(NationalIdentityNumber nin);

  Optional<Person> fetchPersonByIdentifier(String cristinIdentifier);

  /**
   * Creates the person, or returns the existing one when the registry answers that a person with
   * this number already exists.
   */
  Optional<Person> createPerson(NationalIdentityNumber nin, String firstName, String lastName);

  /**
   * Runs the code a lookup uses once without looking anyone up, so that the first lookup does not
   * pay for loading and compiling it. Keeps no connection open afterwards.
   */
  void prime();
}
