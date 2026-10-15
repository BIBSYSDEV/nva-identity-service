package no.unit.nva.useraccessservice.dao;

import static java.util.Objects.isNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.dao.DynamoEntriesUtils.nonEmpty;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Row in the users table with what the pre-token Lambda needs to know about a person. Keyed on the
 * person identifier, so Cognito users for the same person share it. No secondary index keys and an
 * own {@code type} value, so the user indexes and scans never see it.
 */
@DynamoDbBean
public class PersonDao implements DynamoEntryWithRangeKey {

  public static final TableSchema<PersonDao> TABLE_SCHEMA = TableSchema.fromBean(PersonDao.class);
  public static final String TYPE_VALUE = "PERSON";
  public static final String CRISTIN_ID_FIELD = "cristinId";
  public static final String GIVEN_NAME_FIELD = "givenName";
  public static final String FAMILY_NAME_FIELD = "familyName";
  public static final String CUSTOMERS_FIELD = "customers";
  public static final String LAST_VERIFIED_FIELD = "lastVerified";
  public static final String INVALID_PERSON_EMPTY_CRISTIN_ID =
      "Invalid person entry: Empty cristinId is not allowed";

  private URI cristinId;
  private String givenName;
  private String familyName;
  private Set<String> customers;
  private Instant lastVerified;

  public PersonDao() {
    super();
  }

  public static PersonDao fromLinkedPerson(LinkedPerson person) {
    var dao = new PersonDao();
    dao.setCristinId(person.cristinId());
    dao.setGivenName(person.givenName());
    dao.setFamilyName(person.familyName());
    dao.setCustomers(person.customers().stream().map(URI::toString).collect(Collectors.toSet()));
    dao.setLastVerified(person.lastVerified());
    dao.validate();
    return dao;
  }

  public static Key keyFor(URI cristinId) {
    var keyValue = primaryKey(cristinId);
    return Key.builder().partitionValue(keyValue).sortValue(keyValue).build();
  }

  private static String primaryKey(URI cristinId) {
    return String.join(FIELD_DELIMITER, TYPE_VALUE, String.valueOf(cristinId));
  }

  public LinkedPerson toLinkedPerson() {
    var customerIds =
        nonEmpty(customers)
            ? customers.stream().map(URI::create).collect(Collectors.toSet())
            : Set.<URI>of();
    return new LinkedPerson(cristinId, givenName, familyName, customerIds, lastVerified);
  }

  private void validate() {
    if (isNull(cristinId)) {
      throw new InvalidEntryInternalException(INVALID_PERSON_EMPTY_CRISTIN_ID);
    }
  }

  @DynamoDbAttribute(CRISTIN_ID_FIELD)
  public URI getCristinId() {
    return cristinId;
  }

  public void setCristinId(URI cristinId) {
    this.cristinId = cristinId;
  }

  @DynamoDbAttribute(GIVEN_NAME_FIELD)
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @DynamoDbAttribute(FAMILY_NAME_FIELD)
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /** DynamoDB rejects empty sets, so an empty set is stored as an absent attribute. */
  @DynamoDbAttribute(CUSTOMERS_FIELD)
  public Set<String> getCustomers() {
    return nonEmpty(customers) ? customers : null;
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void setCustomers(Set<String> customers) {
    this.customers = nonEmpty(customers) ? customers : null;
  }

  @DynamoDbAttribute(LAST_VERIFIED_FIELD)
  public Instant getLastVerified() {
    return lastVerified;
  }

  public void setLastVerified(Instant lastVerified) {
    this.lastVerified = lastVerified;
  }

  @Override
  @DynamoDbPartitionKey
  @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
  public String getPrimaryKeyHashKey() {
    return primaryKey(cristinId);
  }

  @Override
  @JacocoGenerated
  public void setPrimaryKeyHashKey(String primaryHashKey) {
    // DO NOTHING, the key is derived from the cristinId
  }

  @Override
  @DynamoDbSortKey
  @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
  public String getPrimaryKeyRangeKey() {
    return primaryKey(cristinId);
  }

  @Override
  @JacocoGenerated
  public void setPrimaryKeyRangeKey(String primaryRangeKey) {
    // DO NOTHING, the key is derived from the cristinId
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
    return Objects.hash(cristinId, givenName, familyName, customers, lastVerified);
  }

  @Override
  @JacocoGenerated
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PersonDao other)) {
      return false;
    }
    return Objects.equals(cristinId, other.cristinId)
        && Objects.equals(givenName, other.givenName)
        && Objects.equals(familyName, other.familyName)
        && Objects.equals(customers, other.customers)
        && Objects.equals(lastVerified, other.lastVerified);
  }
}
