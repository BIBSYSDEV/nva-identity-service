package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;

import java.time.Instant;
import java.util.Objects;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Row in the users table that links a Cognito user to a person. The row has no secondary index
 * keys, so it is invisible to the user indexes, and its own {@code type} value keeps it out of the
 * user scans. It holds the person identifier rather than the person id, because the id names the
 * environment it was made in.
 */
@DynamoDbBean
public class LinkDao implements DynamoEntryWithRangeKey {

  public static final TableSchema<LinkDao> TABLE_SCHEMA = TableSchema.fromBean(LinkDao.class);
  public static final String TYPE_VALUE = "LINK";
  public static final String USERNAME_FIELD = "username";
  public static final String PERSON_IDENTIFIER_FIELD = "personIdentifier";
  public static final String FEIDE_IDENTIFIER_FIELD = "feideIdentifier";
  public static final String ORG_FEIDE_DOMAIN_FIELD = "orgFeideDomain";
  public static final String MODIFIED_FIELD = "modified";
  public static final String INVALID_LINK_EMPTY_USERNAME =
      "Invalid link entry: Empty username is not allowed";
  public static final String INVALID_LINK_EMPTY_PERSON_IDENTIFIER =
      "Invalid link entry: Empty personIdentifier is not allowed";

  private String username;
  private String personIdentifier;
  private String feideIdentifier;
  private String orgFeideDomain;
  private Instant modified;

  public LinkDao() {
    super();
  }

  public static Key keyFor(String username) {
    var keyValue = primaryKey(username);
    return Key.builder().partitionValue(keyValue).sortValue(keyValue).build();
  }

  private static String primaryKey(String username) {
    return String.join(FIELD_DELIMITER, TYPE_VALUE, username);
  }

  /**
   * Checked before the row is written and after it is read, so a broken row never admits anyone.
   */
  public void validate() {
    if (StringUtils.isBlank(username)) {
      throw new InvalidEntryInternalException(INVALID_LINK_EMPTY_USERNAME);
    }
    if (StringUtils.isBlank(personIdentifier)) {
      throw new InvalidEntryInternalException(INVALID_LINK_EMPTY_PERSON_IDENTIFIER);
    }
  }

  @DynamoDbAttribute(USERNAME_FIELD)
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @DynamoDbAttribute(PERSON_IDENTIFIER_FIELD)
  public String getPersonIdentifier() {
    return personIdentifier;
  }

  public void setPersonIdentifier(String personIdentifier) {
    this.personIdentifier = personIdentifier;
  }

  @DynamoDbAttribute(FEIDE_IDENTIFIER_FIELD)
  public String getFeideIdentifier() {
    return feideIdentifier;
  }

  public void setFeideIdentifier(String feideIdentifier) {
    this.feideIdentifier = feideIdentifier;
  }

  @DynamoDbAttribute(ORG_FEIDE_DOMAIN_FIELD)
  public String getOrgFeideDomain() {
    return orgFeideDomain;
  }

  public void setOrgFeideDomain(String orgFeideDomain) {
    this.orgFeideDomain = orgFeideDomain;
  }

  @DynamoDbAttribute(MODIFIED_FIELD)
  public Instant getModified() {
    return modified;
  }

  public void setModified(Instant modified) {
    this.modified = modified;
  }

  @Override
  @DynamoDbPartitionKey
  @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
  public String getPrimaryKeyHashKey() {
    return primaryKey(username);
  }

  @Override
  @JacocoGenerated
  public void setPrimaryKeyHashKey(String primaryHashKey) {
    // DO NOTHING, the key is derived from the username
  }

  @Override
  @DynamoDbSortKey
  @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
  public String getPrimaryKeyRangeKey() {
    return primaryKey(username);
  }

  @Override
  @JacocoGenerated
  public void setPrimaryKeyRangeKey(String primaryRangeKey) {
    // DO NOTHING, the key is derived from the username
  }

  @Override
  @DynamoDbAttribute(TYPE_FIELD)
  public String getType() {
    return TYPE_VALUE;
  }

  @Override
  @JacocoGenerated
  public void setType(String ignored) {
    // DO NOTHING, the type is fixed
  }

  @Override
  @JacocoGenerated
  public int hashCode() {
    return Objects.hash(username, personIdentifier, feideIdentifier, orgFeideDomain, modified);
  }

  @Override
  @JacocoGenerated
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LinkDao other)) {
      return false;
    }
    return Objects.equals(username, other.username)
        && Objects.equals(personIdentifier, other.personIdentifier)
        && Objects.equals(feideIdentifier, other.feideIdentifier)
        && Objects.equals(orgFeideDomain, other.orgFeideDomain)
        && Objects.equals(modified, other.modified);
  }
}
