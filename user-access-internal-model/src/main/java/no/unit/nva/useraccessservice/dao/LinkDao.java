package no.unit.nva.useraccessservice.dao;

import static java.util.Objects.isNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.IdentityLink;
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
 * user scans.
 */
@DynamoDbBean
public class LinkDao implements DynamoEntryWithRangeKey {

  public static final TableSchema<LinkDao> TABLE_SCHEMA = TableSchema.fromBean(LinkDao.class);
  public static final String TYPE_VALUE = "LINK";
  public static final String USERNAME_FIELD = "username";
  public static final String CRISTIN_ID_FIELD = "cristinId";
  public static final String FEIDE_IDENTIFIER_FIELD = "feideIdentifier";
  public static final String ORG_FEIDE_DOMAIN_FIELD = "orgFeideDomain";
  public static final String MODIFIED_FIELD = "modified";
  public static final String INVALID_LINK_EMPTY_USERNAME =
      "Invalid link entry: Empty username is not allowed";
  public static final String INVALID_LINK_EMPTY_CRISTIN_ID =
      "Invalid link entry: Empty cristinId is not allowed";

  private String username;
  private URI cristinId;
  private String feideIdentifier;
  private String orgFeideDomain;
  private Instant modified;

  public LinkDao() {
    super();
  }

  public static LinkDao fromIdentityLink(IdentityLink link) {
    var dao = new LinkDao();
    dao.setUsername(link.username());
    dao.setCristinId(link.cristinId());
    dao.setFeideIdentifier(link.feideIdentifier());
    dao.setOrgFeideDomain(link.orgFeideDomain());
    dao.setModified(link.modified());
    dao.validate();
    return dao;
  }

  public static Key keyFor(String username) {
    var keyValue = primaryKey(username);
    return Key.builder().partitionValue(keyValue).sortValue(keyValue).build();
  }

  private static String primaryKey(String username) {
    return String.join(FIELD_DELIMITER, TYPE_VALUE, username);
  }

  public IdentityLink toIdentityLink() {
    return new IdentityLink(username, cristinId, feideIdentifier, orgFeideDomain, modified);
  }

  private void validate() {
    if (StringUtils.isBlank(username)) {
      throw new InvalidEntryInternalException(INVALID_LINK_EMPTY_USERNAME);
    }
    if (isNull(cristinId)) {
      throw new InvalidEntryInternalException(INVALID_LINK_EMPTY_CRISTIN_ID);
    }
  }

  @DynamoDbAttribute(USERNAME_FIELD)
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @DynamoDbAttribute(CRISTIN_ID_FIELD)
  public URI getCristinId() {
    return cristinId;
  }

  public void setCristinId(URI cristinId) {
    this.cristinId = cristinId;
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
    return Objects.hash(username, cristinId, feideIdentifier, orgFeideDomain, modified);
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
        && Objects.equals(cristinId, other.cristinId)
        && Objects.equals(feideIdentifier, other.feideIdentifier)
        && Objects.equals(orgFeideDomain, other.orgFeideDomain)
        && Objects.equals(modified, other.modified);
  }
}
