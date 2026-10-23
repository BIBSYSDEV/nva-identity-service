package no.unit.nva.database;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.database.IdentityService.Constants.USERS_AND_ROLES_TABLE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccessservice.dao.LinkDao;
import no.unit.nva.useraccessservice.dao.PersonDao;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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
    var link = new IdentityLink(randomString(), randomPersonId(), null, null, Instant.now());

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
  void shouldStoreThePersonIdentifierRatherThanThePersonId() {
    var link = randomLink(randomString());

    identityService.saveLink(link);

    var row = readRow(LinkDao.TYPE_VALUE, link.username());
    assertThat(
        row.get(LinkDao.PERSON_IDENTIFIER_FIELD).s(), is(equalTo(identifierOf(link.personId()))));
  }

  @Test
  void shouldRejectLinkWhosePersonIdHasNoIdentifier() {
    var link = new IdentityLink(randomString(), URI.create("https://localhost"), null, null, null);

    assertThrows(InvalidEntryInternalException.class, () -> identityService.saveLink(link));
  }

  @Test
  void shouldRejectLinkRowWithoutPersonIdentifier() {
    var username = randomString();
    writeRow(
        LinkDao.TYPE_VALUE,
        username,
        Map.of(LinkDao.USERNAME_FIELD, AttributeValue.fromS(username)));

    assertThrows(InvalidEntryInternalException.class, () -> identityService.fetchLink(username));
  }

  @Test
  void shouldReturnEmptyWhenNoPersonIsLinked() {
    assertThat(identityService.fetchLinkedPerson(randomPersonId()), is(Optional.empty()));
  }

  @Test
  void shouldStoreAndFetchLinkedPerson() {
    var person = randomPerson(randomPersonId(), Set.of(randomCustomerId(), randomCustomerId()));

    identityService.saveLinkedPerson(person);

    assertThat(identityService.fetchLinkedPerson(person.personId()), is(Optional.of(person)));
  }

  @Test
  void shouldStoreLinkedPersonWithoutCustomers() {
    var person = randomPerson(randomPersonId(), Set.of());

    identityService.saveLinkedPerson(person);

    var fetched = identityService.fetchLinkedPerson(person.personId()).orElseThrow();
    assertThat(fetched.customers(), is(empty()));
    assertThat(fetched, is(equalTo(person)));
  }

  @Test
  void shouldOverwriteLinkedPersonWhenSavedAgain() {
    var personId = randomPersonId();
    identityService.saveLinkedPerson(randomPerson(personId, Set.of(randomCustomerId())));
    var updated = randomPerson(personId, Set.of(randomCustomerId()));

    identityService.saveLinkedPerson(updated);

    assertThat(identityService.fetchLinkedPerson(personId), is(Optional.of(updated)));
  }

  @Test
  void shouldStoreTheIdentifiersOfThePersonAndTheCustomersRatherThanTheirIds() {
    var firstCustomer = randomCustomerId();
    var secondCustomer = randomCustomerId();
    var person = randomPerson(randomPersonId(), Set.of(firstCustomer, secondCustomer));

    identityService.saveLinkedPerson(person);

    var row = readRow(PersonDao.TYPE_VALUE, identifierOf(person.personId()));
    assertThat(
        row.get(PersonDao.PERSON_IDENTIFIER_FIELD).s(),
        is(equalTo(identifierOf(person.personId()))));
    assertThat(
        row.get(PersonDao.CUSTOMERS_FIELD).ss(),
        containsInAnyOrder(identifierOf(firstCustomer), identifierOf(secondCustomer)));
  }

  @Test
  void shouldRejectPersonRowWithoutPersonIdentifier() {
    var personId = randomPersonId();
    writeRow(PersonDao.TYPE_VALUE, identifierOf(personId), Map.of());

    assertThrows(
        InvalidEntryInternalException.class, () -> identityService.fetchLinkedPerson(personId));
  }

  @Test
  void shouldKeepLinkAndPersonRowsOutOfUserQueries() throws ConflictException {
    var personId = randomPersonId();
    var institution = randomUri();
    var user =
        UserDto.newBuilder()
            .withUsername(randomString())
            .withInstitution(institution)
            .withCristinId(personId)
            .withInstitutionCristinId(randomCristinOrgId())
            .build();
    identityService.addUser(user);
    identityService.saveLink(new IdentityLink(randomString(), personId, null, null, Instant.now()));
    identityService.saveLinkedPerson(randomPerson(personId, Set.of(randomCustomerId())));

    var onePage = identityService.fetchOnePageOfUsers(scanEverything()).getRetrievedUsers();

    assertThat(usernames(identityService.listAllUsers()), contains(user.getUsername()));
    assertThat(usernames(onePage), contains(user.getUsername()));
    assertThat(usernames(identityService.listUsers(institution)), contains(user.getUsername()));
    assertThat(
        usernames(identityService.getUsersByCristinId(personId)), contains(user.getUsername()));
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
    return new IdentityLink(
        username, randomPersonId(), randomString(), randomString(), Instant.now());
  }

  private static LinkedPerson randomPerson(URI personId, Set<URI> customers) {
    return new LinkedPerson(personId, randomString(), randomString(), customers, Instant.now());
  }

  /** A person id as the person registry client builds it. */
  private static URI randomPersonId() {
    return new UriWrapper("https", API_DOMAIN)
        .addChild("cristin", "person", randomString())
        .getUri();
  }

  /** A customer id as the customer service builds it. */
  private static URI randomCustomerId() {
    return UriWrapper.fromUri(ENVIRONMENT.readEnv("ID_NAMESPACE"))
        .addChild(UUID.randomUUID().toString())
        .getUri();
  }

  private static String identifierOf(URI id) {
    return UriWrapper.fromUri(id).getLastPathElement();
  }

  private static String keyOf(String type, String identifier) {
    return type + "#" + identifier;
  }

  private Map<String, AttributeValue> readRow(String type, String identifier) {
    var key = AttributeValue.fromS(keyOf(type, identifier));
    var request =
        GetItemRequest.builder()
            .tableName(USERS_AND_ROLES_TABLE)
            .key(Map.of(PRIMARY_KEY_HASH_KEY, key, PRIMARY_KEY_RANGE_KEY, key))
            .build();
    return localDynamo.getItem(request).item();
  }

  private void writeRow(String type, String identifier, Map<String, AttributeValue> attributes) {
    var key = AttributeValue.fromS(keyOf(type, identifier));
    var item = new HashMap<>(attributes);
    item.put(PRIMARY_KEY_HASH_KEY, key);
    item.put(PRIMARY_KEY_RANGE_KEY, key);
    item.put("type", AttributeValue.fromS(type));
    localDynamo.putItem(
        PutItemRequest.builder().tableName(USERS_AND_ROLES_TABLE).item(item).build());
  }
}
