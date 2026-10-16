package no.unit.nva.database;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.database.IdentityService.Constants;
import no.unit.nva.useraccessservice.dao.LinkDao;
import no.unit.nva.useraccessservice.dao.PersonDao;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Stores the link from a Cognito user to a person, and what is known about the person, in the users
 * table. Reads are strongly consistent because a login writes the rows and the next token
 * generation reads them right after.
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
    links.putItem(LinkDao.fromIdentityLink(link));
  }

  public Optional<IdentityLink> fetchLink(String username) {
    var item = links.getItem(request -> request.key(LinkDao.keyFor(username)).consistentRead(true));
    return Optional.ofNullable(item).map(LinkDao::toIdentityLink);
  }

  public void savePerson(LinkedPerson person) {
    persons.putItem(PersonDao.fromLinkedPerson(person));
  }

  public Optional<LinkedPerson> fetchPerson(URI cristinId) {
    var item =
        persons.getItem(request -> request.key(PersonDao.keyFor(cristinId)).consistentRead(true));
    return Optional.ofNullable(item).map(PersonDao::toLinkedPerson);
  }
}
