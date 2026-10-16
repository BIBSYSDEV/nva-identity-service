package no.unit.nva.database;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkServiceTest extends LocalIdentityService {

  private static final int PAGE_SIZE = 100;
  private IdentityService identityService;

  @BeforeEach
  void init() {
    identityService = createDatabaseServiceUsingLocalStorage();
  }

  @Test
  void shouldReturnEmptyWhenNoLinkExistsForUsername() {
    assertThat(identityService.fetchLink(randomString()), is(Optional.empty()));
  }

  @Test
  void shouldStoreAndFetchLinkForCognitoUsername() {
    var link = randomLink(randomString());

    identityService.saveLink(link);

    assertThat(identityService.fetchLink(link.username()), is(Optional.of(link)));
  }

  @Test
  void shouldStoreLinkWithoutFeideFields() {
    var link = new IdentityLink(randomString(), randomUri(), null, null, Instant.now());

    identityService.saveLink(link);

    assertThat(identityService.fetchLink(link.username()), is(Optional.of(link)));
  }

  @Test
  void shouldOverwriteLinkWhenSavedAgainForSameUsername() {
    var first = randomLink(randomString());
    identityService.saveLink(first);
    var second = randomLink(first.username());

    identityService.saveLink(second);

    assertThat(identityService.fetchLink(first.username()), is(Optional.of(second)));
  }

  @Test
  void shouldRejectLinkWithoutUsername() {
    var link = new IdentityLink(" ", randomUri(), null, null, Instant.now());

    assertThrows(InvalidEntryInternalException.class, () -> identityService.saveLink(link));
  }

  @Test
  void shouldRejectLinkWithoutCristinId() {
    var link = new IdentityLink(randomString(), null, null, null, Instant.now());

    assertThrows(InvalidEntryInternalException.class, () -> identityService.saveLink(link));
  }

  @Test
  void shouldReturnEmptyWhenNoPersonIsLinked() {
    assertThat(identityService.fetchLinkedPerson(randomUri()), is(Optional.empty()));
  }

  @Test
  void shouldStoreAndFetchLinkedPerson() {
    var firstCustomer = randomUri();
    var secondCustomer = randomUri();
    var person = randomPerson(randomUri(), Set.of(firstCustomer, secondCustomer));

    identityService.saveLinkedPerson(person);

    assertThat(identityService.fetchLinkedPerson(person.cristinId()), is(Optional.of(person)));
  }

  @Test
  void shouldStoreLinkedPersonWithoutCustomers() {
    var person = randomPerson(randomUri(), Set.of());

    identityService.saveLinkedPerson(person);

    var fetched = identityService.fetchLinkedPerson(person.cristinId()).orElseThrow();
    assertThat(fetched.customers(), is(empty()));
    assertThat(fetched, is(equalTo(person)));
  }

  @Test
  void shouldOverwriteLinkedPersonWhenSavedAgain() {
    var cristinId = randomUri();
    identityService.saveLinkedPerson(randomPerson(cristinId, Set.of(randomUri())));
    var updated = randomPerson(cristinId, Set.of(randomUri()));

    identityService.saveLinkedPerson(updated);

    assertThat(identityService.fetchLinkedPerson(cristinId), is(Optional.of(updated)));
  }

  @Test
  void shouldRejectLinkedPersonWithoutCristinId() {
    var person = new LinkedPerson(null, randomString(), randomString(), Set.of(), Instant.now());

    assertThrows(
        InvalidEntryInternalException.class, () -> identityService.saveLinkedPerson(person));
  }

  @Test
  void shouldKeepLinkAndPersonRowsOutOfUserQueries() throws ConflictException {
    var cristinId = randomUri();
    var institution = randomUri();
    var user =
        UserDto.newBuilder()
            .withUsername(randomString())
            .withInstitution(institution)
            .withCristinId(cristinId)
            .withInstitutionCristinId(randomCristinOrgId())
            .build();
    identityService.addUser(user);
    identityService.saveLink(
        new IdentityLink(randomString(), cristinId, null, null, Instant.now()));
    identityService.saveLinkedPerson(randomPerson(cristinId, Set.of(institution)));

    var onePage = identityService.fetchOnePageOfUsers(scanEverything()).getRetrievedUsers();

    assertThat(usernames(identityService.listAllUsers()), contains(user.getUsername()));
    assertThat(usernames(onePage), contains(user.getUsername()));
    assertThat(usernames(identityService.listUsers(institution)), contains(user.getUsername()));
    assertThat(
        usernames(identityService.getUsersByCristinId(cristinId)), contains(user.getUsername()));
  }

  @Test
  void shouldFetchUserByUsernameWithConsistentRead() throws ConflictException {
    var user =
        UserDto.newBuilder()
            .withUsername(randomString())
            .withInstitution(randomUri())
            .withCristinId(randomUri())
            .withInstitutionCristinId(randomCristinOrgId())
            .build();
    identityService.addUser(user);

    var fetched = identityService.fetchUserConsistently(user.getUsername());

    assertThat(fetched.map(UserDto::getUsername), is(Optional.of(user.getUsername())));
    assertThat(identityService.fetchUserConsistently(randomString()), is(Optional.empty()));
  }

  private static ScanDatabaseRequestV2 scanEverything() {
    return new ScanDatabaseRequestV2(randomString(), PAGE_SIZE, null);
  }

  private static List<String> usernames(Collection<UserDto> users) {
    return users.stream().map(UserDto::getUsername).toList();
  }

  private static IdentityLink randomLink(String username) {
    return new IdentityLink(username, randomUri(), randomString(), randomString(), Instant.now());
  }

  private static LinkedPerson randomPerson(URI cristinId, Set<URI> customers) {
    return new LinkedPerson(cristinId, randomString(), randomString(), customers, Instant.now());
  }
}
