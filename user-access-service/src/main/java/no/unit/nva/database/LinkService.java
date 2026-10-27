package no.unit.nva.database;

import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityService.Constants;
import no.unit.nva.useraccessservice.dao.LinkDao;
import no.unit.nva.useraccessservice.dao.PersonDao;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Stores the link from a Cognito user to a person, and what is known about the person, in the users
 * table. Reads are strongly consistent because a login writes the rows and the next token
 * generation reads them right after.
 *
 * <p>The rows hold identifiers, not ids, because an id names the environment it was made in. On the
 * way out the person id is rebuilt the way the person registry client builds it, and a customer id
 * the way the customer service builds it.
 */
public class LinkService extends DatabaseSubService {

  private final DynamoDbTable<LinkDao> links;
  private final DynamoDbTable<PersonDao> persons;

  protected LinkService(DynamoDbClient client) {
    super(client);
    this.links = this.client.table(Constants.USERS_AND_ROLES_TABLE, LinkDao.TABLE_SCHEMA);
    this.persons = this.client.table(Constants.USERS_AND_ROLES_TABLE, PersonDao.TABLE_SCHEMA);
  }

  public void saveLink(IdentityLink link) {
    links.putItem(toLinkRow(link));
  }

  public Optional<IdentityLink> fetchLink(String username) {
    var row = links.getItem(request -> request.key(LinkDao.keyFor(username)).consistentRead(true));
    return Optional.ofNullable(row).map(LinkService::toIdentityLink);
  }

  public void savePerson(LinkedPerson person) {
    persons.putItem(toPersonRow(person));
  }

  public Optional<LinkedPerson> fetchPerson(URI personId) {
    var key = PersonDao.keyFor(identifierOf(personId));
    var row = persons.getItem(request -> request.key(key).consistentRead(true));
    return Optional.ofNullable(row).map(LinkService::toLinkedPerson);
  }

  private static LinkDao toLinkRow(IdentityLink link) {
    var row = new LinkDao();
    row.setUsername(link.username());
    row.setPersonIdentifier(identifierOf(link.personId()));
    row.setFeideIdentifier(link.feideIdentifier());
    row.setOrgFeideDomain(link.orgFeideDomain());
    row.setModified(link.modified());
    row.validate();
    return row;
  }

  private static IdentityLink toIdentityLink(LinkDao row) {
    row.validate();
    return new IdentityLink(
        row.getUsername(),
        personId(row.getPersonIdentifier()),
        row.getFeideIdentifier(),
        row.getOrgFeideDomain(),
        row.getModified());
  }

  private static PersonDao toPersonRow(LinkedPerson person) {
    var row = new PersonDao();
    row.setPersonIdentifier(identifierOf(person.personId()));
    row.setGivenName(person.givenName());
    row.setFamilyName(person.familyName());
    row.setCustomers(
        person.customers().stream().map(LinkService::identifierOf).collect(Collectors.toSet()));
    row.setLastVerified(person.lastVerified());
    row.validate();
    return row;
  }

  private static LinkedPerson toLinkedPerson(PersonDao row) {
    row.validate();
    var customers =
        Optional.ofNullable(row.getCustomers()).orElse(Set.of()).stream()
            .map(LinkService::customerId)
            .collect(Collectors.toSet());
    return new LinkedPerson(
        personId(row.getPersonIdentifier()),
        row.getGivenName(),
        row.getFamilyName(),
        customers,
        row.getLastVerified());
  }

  private static String identifierOf(URI id) {
    var path = UriWrapper.fromUri(id).getPath();
    if (path.isEmptyPath()) {
      throw new InvalidEntryInternalException("Not an id with an identifier: " + id);
    }
    return path.getLastPathElement();
  }

  private static URI personId(String identifier) {
    return new UriWrapper("https", API_DOMAIN).addChild("cristin", "person", identifier).getUri();
  }

  private static URI customerId(String identifier) {
    return UriWrapper.fromUri(ENVIRONMENT.readEnv("ID_NAMESPACE")).addChild(identifier).getUri();
  }
}
