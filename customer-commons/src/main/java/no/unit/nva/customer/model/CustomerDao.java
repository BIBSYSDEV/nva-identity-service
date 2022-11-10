package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.ApplicationDomain.fromUri;
import static no.unit.nva.customer.model.dynamo.converters.DynamoUtils.nonEmpty;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_ORG_DOMAIN_INDEX_NAME;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.dynamo.converters.DoiAgentConverter;
import no.unit.nva.customer.model.dynamo.converters.VocabularyConverterProvider;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.Typed;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean(converterProviders = {VocabularyConverterProvider.class, DefaultAttributeConverterProvider.class})
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.TooManyFields"})
public class CustomerDao implements Typed {

    public static final String IDENTIFIER = "identifier";
    public static final String ORG_DOMAIN = "feideOrganizationDomain";
    public static final String CRISTIN_ID = "cristinId";
    public static final String TYPE = "Customer";
    public static final Set<VocabularyDao> EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO = null;
    public static final TableSchema<CustomerDao> TABLE_SCHEMA = TableSchema.fromClass(CustomerDao.class);
    public static final String VOCABULARIES_FIELD = "vocabularies";
    private UUID identifier;
    private Instant createdDate;
    private Instant modifiedDate;
    private String name;
    private String displayName;
    private String shortName;
    private String archiveName;
    private String cname;
    private String institutionDns;
    private String feideOrganizationDomain;
    private URI cristinId;
    private URI customerOf;
    private Set<VocabularyDao> vocabularies;
    private URI rorId;
    private PublicationWorkflow publicationWorkflow;
    private DoiAgentDao doiAgent;

    public CustomerDao() {
        vocabularies = EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
        publicationWorkflow = PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CustomerDao fromCustomerDto(CustomerDto dto) {
        return builder()
                   .withArchiveName(dto.getArchiveName())
                   .withCname(dto.getCname())
                   .withCreatedDate(dto.getCreatedDate())
                   .withCristinId(dto.getCristinId())
                   .withCustomerOf(extractCustomerOf(dto))
                   .withDisplayName(dto.getDisplayName())
                   .withIdentifier(dto.getIdentifier())
                   .withInstitutionDns(dto.getInstitutionDns())
                   .withShortName(dto.getShortName())
                   .withFeideOrganizationDomain(dto.getFeideOrganizationDomain())
                   .withModifiedDate(dto.getModifiedDate())
                   .withVocabularySettings(extractVocabularySettings(dto))
                   .withName(dto.getName())
                   .withRorId(dto.getRorId())
                   .withPublicationWorkflow(dto.getPublicationWorkflow())
                   .withDoiAgent(dto.getDoiAgent())
                   .build();
    }

    @DynamoDbAttribute(IDENTIFIER)
    @DynamoDbPartitionKey
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

    @DynamoDbSecondaryPartitionKey(indexNames = {BY_ORG_DOMAIN_INDEX_NAME})
    @DynamoDbAttribute(ORG_DOMAIN)
    public String getFeideOrganizationDomain() {
        return feideOrganizationDomain;
    }

    public void setFeideOrganizationDomain(String feideOrganizationDomain) {
        this.feideOrganizationDomain = feideOrganizationDomain;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {BY_CRISTIN_ID_INDEX_NAME})
    @DynamoDbAttribute(CRISTIN_ID)
    public URI getCristinId() {
        return cristinId;
    }

    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    public URI getCustomerOf() {
        return customerOf;
    }

    public void setCustomerOf(URI customerOf) {
        this.customerOf = customerOf;
    }

    @DynamoDbIgnoreNulls
    @DynamoDbAttribute(VOCABULARIES_FIELD)
    public Set<VocabularyDao> getVocabularies() {
        return nonEmpty(vocabularies) ? vocabularies : EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public void setVocabularies(Set<VocabularyDao> vocabularies) {
        this.vocabularies = nonEmpty(vocabularies) ? vocabularies : EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public URI getRorId() {
        return rorId;
    }

    public void setRorId(URI rorId) {
        this.rorId = rorId;
    }

    public PublicationWorkflow getPublicationWorkflow() {
        return publicationWorkflow;
    }

    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }

    @DynamoDbConvertedBy(DoiAgentConverter.class)
    public DoiAgentDao getDoiAgent() {
        return doiAgent;
    }

    public void setDoiAgent(DoiAgentDao doi) {
        this.doiAgent = doi;
    }

    public CustomerDto toCustomerDto() {
        CustomerDto customerDto =
            CustomerDto.builder()
                .withCname(getCname())
                .withName(getName())
                .withIdentifier(getIdentifier())
                .withArchiveName(getArchiveName())
                .withCreatedDate(getCreatedDate())
                .withDisplayName(getDisplayName())
                .withInstitutionDns(getInstitutionDns())
                .withShortName(getShortName())
                .withVocabularies(extractVocabularySettings())
                .withModifiedDate(getModifiedDate())
                .withFeideOrganizationDomain(getFeideOrganizationDomain())
                .withCristinId(getCristinId())
                .withCustomerOf(fromUri(getCustomerOf()))
                .withRorId(getRorId())
                .withPublicationWorkflow(getPublicationWorkflow())
                .withDoiAgent(getDoiAgent())
                .build();
        return LinkedDataContextUtils.addContextAndId(customerDto);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerDao that = (CustomerDao) o;
        return Objects.equals(identifier, that.identifier)
               && Objects.equals(createdDate, that.createdDate)
               && Objects.equals(modifiedDate, that.modifiedDate)
               && Objects.equals(name, that.name)
               && Objects.equals(displayName, that.displayName)
               && Objects.equals(shortName, that.shortName)
               && Objects.equals(archiveName, that.archiveName)
               && Objects.equals(cname, that.cname)
               && Objects.equals(institutionDns, that.institutionDns)
               && Objects.equals(feideOrganizationDomain, that.feideOrganizationDomain)
               && Objects.equals(cristinId, that.cristinId)
               && Objects.equals(customerOf, that.customerOf)
               && Objects.equals(vocabularies, that.vocabularies)
               && Objects.equals(rorId, that.rorId)
               && publicationWorkflow == that.publicationWorkflow
               && Objects.equals(doiAgent, that.doiAgent);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(identifier, createdDate, modifiedDate, name, displayName, shortName, archiveName, cname,
                            institutionDns, feideOrganizationDomain, cristinId, customerOf, vocabularies, rorId,
                            publicationWorkflow, doiAgent);
    }

    private static URI extractCustomerOf(CustomerDto dto) {
        return Optional.ofNullable(dto)
                   .map(CustomerDto::getCustomerOf)
                   .map(ApplicationDomain::toString)
                   .map(URI::create)
                   .orElse(null);
    }

    @Override
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

        public Builder withCreatedDate(String createdDate) {
            customerDb.setCreatedDate(
                (createdDate == null) ? null : Instant.parse(createdDate)
            );
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDb.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withModifiedDate(String modifiedDate) {
            customerDb.setModifiedDate(
                (modifiedDate == null) ? null : Instant.parse(modifiedDate)
            );
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

        public Builder withFeideOrganizationDomain(String feideOrganizationDomain) {
            customerDb.setFeideOrganizationDomain(feideOrganizationDomain);
            return this;
        }

        public Builder withCristinId(URI cristinId) {
            customerDb.setCristinId(cristinId);
            return this;
        }

        public Builder withCustomerOf(URI customerOf) {
            customerDb.setCustomerOf(customerOf);
            return this;
        }

        public Builder withVocabularySettings(Set<VocabularyDao> vocabularySettings) {
            customerDb.setVocabularies(vocabularySettings);
            return this;
        }

        public Builder withRorId(URI rorId) {
            customerDb.setRorId(rorId);
            return this;
        }

        public Builder withPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
            customerDb.setPublicationWorkflow(publicationWorkflow);
            return this;
        }

        public Builder withDoiAgent(DoiAgent doiAgent) {
            customerDb.setDoiAgent(doiAgent == null ? null : new DoiAgentDao(doiAgent));
            return this;
        }

        public CustomerDao build() {
            return customerDb;
        }
    }

    @DynamoDbBean
    public static class DoiAgentDao implements DoiAgent {

        private String prefix;
        private String name;

        public DoiAgentDao() {
        }

        public DoiAgentDao(DoiAgent doiAgent) {
            this.prefix = doiAgent.getPrefix();
            this.name = doiAgent.getName();
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        @JacocoGenerated
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DoiAgentDao that = (DoiAgentDao) o;
            return Objects.equals(prefix, that.prefix) && Objects.equals(name, that.name);
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(prefix, name);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", DoiAgentDao.class.getSimpleName() + "[", "]")
                       .add("prefix='" + prefix + "'")
                       .add("name='" + name + "'")
                       .toString();
        }
    }
}