package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.dynamo.converters.DynamoUtils.nonEmpty;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_ORG_NUMBER_INDEX_NAME;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.dynamo.converters.VocabularyConverterProvider;
import no.unit.nva.customer.model.interfaces.Typed;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean(converterProviders = {VocabularyConverterProvider.class, DefaultAttributeConverterProvider.class})
@SuppressWarnings("PMD.ExcessivePublicCount")
public class CustomerDao implements Typed {

    public static final String IDENTIFIER = "identifier";
    public static final String ORG_NUMBER = "feideOrganizationId";
    public static final String CRISTIN_ID = "cristinId";
    public static final String TYPE = "Customer";
    public static final Set<VocabularyDao> EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO = null;
    public static final TableSchema<CustomerDao> TABLE_SCHEMA = TableSchema.fromClass(CustomerDao.class);
    private UUID identifier;
    private Instant createdDate;
    private Instant modifiedDate;
    private String name;
    private String displayName;
    private String shortName;
    private String archiveName;
    private String cname;
    private String institutionDns;
    private String feideOrganizationId;
    private String cristinId;
    private Set<VocabularyDao> vocabularies;

    public CustomerDao() {
        vocabularies = EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CustomerDao fromCustomerDto(CustomerDto dto) {
        return builder().withArchiveName(dto.getArchiveName())
            .withCname(dto.getCname())
            .withCreatedDate(Instant.parse(dto.getCreatedDate()))
            .withCristinId(dto.getCristinId())
            .withDisplayName(dto.getDisplayName())
            .withIdentifier(dto.getIdentifier())
            .withInstitutionDns(dto.getInstitutionDns())
            .withShortName(dto.getShortName())
            .withFeideOrganizationId(dto.getFeideOrganizationId())
            .withModifiedDate(Instant.parse(dto.getModifiedDate()))
            .withVocabularySettings(extractVocabularySettings(dto))
            .withName(dto.getName())
            .build();
    }


    @DynamoDbPartitionKey
    @DynamoDbAttribute(IDENTIFIER)
    public UUID getIdentifier() {
        return identifier;
    }


    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }


    public Instant getCreatedDate() {
        return createdDate;
    }


    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }


    public Instant getModifiedDate() {
        return modifiedDate;
    }


    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getDisplayName() {
        return displayName;
    }


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public String getShortName() {
        return shortName;
    }


    public void setShortName(String shortName) {
        this.shortName = shortName;
    }


    public String getArchiveName() {
        return archiveName;
    }


    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }


    public String getCname() {
        return cname;
    }


    public void setCname(String cname) {
        this.cname = cname;
    }


    public String getInstitutionDns() {
        return institutionDns;
    }


    public void setInstitutionDns(String institutionDns) {
        this.institutionDns = institutionDns;
    }


    @DynamoDbSecondaryPartitionKey(indexNames = {BY_ORG_NUMBER_INDEX_NAME})
    @DynamoDbAttribute(ORG_NUMBER)
    public String getFeideOrganizationId() {
        return feideOrganizationId;
    }


    public void setFeideOrganizationId(String feideOrganizationId) {
        this.feideOrganizationId = feideOrganizationId;
    }


    @DynamoDbSecondaryPartitionKey(indexNames = {BY_CRISTIN_ID_INDEX_NAME})
    @DynamoDbAttribute(CRISTIN_ID)
    public String getCristinId() {
        return cristinId;
    }


    public void setCristinId(String cristinId) {
        this.cristinId = cristinId;
    }


    @DynamoDbIgnoreNulls
    public Set<VocabularyDao> getVocabularies() {
        return nonEmpty(vocabularies) ? vocabularies : EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }


    public void setVocabularies(Set<VocabularyDao> vocabularies) {
        this.vocabularies = nonEmpty(vocabularies) ? vocabularies : EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }


    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                            getShortName(), getArchiveName(), getCname(), getInstitutionDns(), getFeideOrganizationId(),
                            getCristinId(), getVocabularies());
    }


    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerDao that = (CustomerDao) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getName(), that.getName())
               && Objects.equals(getDisplayName(), that.getDisplayName())
               && Objects.equals(getShortName(), that.getShortName())
               && Objects.equals(getArchiveName(), that.getArchiveName())
               && Objects.equals(getCname(), that.getCname())
               && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
               && Objects.equals(getFeideOrganizationId(), that.getFeideOrganizationId())
               && Objects.equals(getCristinId(), that.getCristinId())
               && Objects.equals(getVocabularies(), that.getVocabularies());
    }

    public CustomerDto toCustomerDto() {
        CustomerDto customerDto = CustomerDto.builder()
            .withCname(this.getCname())
            .withName(getName())
            .withIdentifier(this.getIdentifier())
            .withArchiveName(this.getArchiveName())
            .withCreatedDate(Optional.ofNullable(this.getCreatedDate()).map(Objects::toString).orElse(null))
            .withDisplayName(this.getDisplayName())
            .withInstitutionDns(this.getInstitutionDns())
            .withShortName(this.getShortName())
            .withVocabularies(extractVocabularySettings())
            .withModifiedDate(Optional.ofNullable(getModifiedDate()).map(Objects::toString).orElse(null))
            .withFeideOrganizationId(getFeideOrganizationId())
            .withCristinId(getCristinId())
            .build();
        return LinkedDataContextUtils.addContextAndId(customerDto);
    }


    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public void setType(String type) {
        if (nonNull(type) && !TYPE.equals(type)) {
            throw new IllegalStateException("Wrong type for Customer:" + type);
        }
    }

    private static Set<VocabularyDao> extractVocabularySettings(CustomerDto dto) {
        return Optional.ofNullable(dto.getVocabularies())
            .stream()
            .flatMap(Collection::stream)
            .map(VocabularyDao::fromVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    private Set<VocabularyDto> extractVocabularySettings() {
        return Optional.ofNullable(this.getVocabularies())
            .stream()
            .flatMap(Collection::stream)
            .map(VocabularyDao::toVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    public static final class Builder {

        private final CustomerDao customerDb;

        public Builder() {
            customerDb = new CustomerDao();
        }

        public Builder withIdentifier(UUID identifier) {
            customerDb.setIdentifier(identifier);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            customerDb.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDb.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withName(String name) {
            customerDb.setName(name);
            return this;
        }

        public Builder withDisplayName(String displayName) {
            customerDb.setDisplayName(displayName);
            return this;
        }

        public Builder withShortName(String shortName) {
            customerDb.setShortName(shortName);
            return this;
        }

        public Builder withArchiveName(String archiveName) {
            customerDb.setArchiveName(archiveName);
            return this;
        }

        public Builder withCname(String cname) {
            customerDb.setCname(cname);
            return this;
        }

        public Builder withInstitutionDns(String institutionDns) {
            customerDb.setInstitutionDns(institutionDns);
            return this;
        }

        public Builder withFeideOrganizationId(String feideOrganizationId) {
            customerDb.setFeideOrganizationId(feideOrganizationId);
            return this;
        }

        public Builder withCristinId(String cristinId) {
            customerDb.setCristinId(cristinId);
            return this;
        }

        public Builder withVocabularySettings(Set<VocabularyDao> vocabularySettings) {
            customerDb.setVocabularies(vocabularySettings);
            return this;
        }

        public CustomerDao build() {
            return customerDb;
        }
    }
}
