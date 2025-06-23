package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.customer.model.ApplicationDomain.fromUri;
import static no.unit.nva.customer.model.dynamo.converters.DynamoUtils.nonEmpty;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.BY_ORG_DOMAIN_INDEX_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.CustomerDto.ServiceCenter;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDao;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.dynamo.converters.DoiAgentConverter;
import no.unit.nva.customer.model.dynamo.converters.RightsRetentionStrategyConverter;
import no.unit.nva.customer.model.dynamo.converters.VocabularyConverterProvider;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import no.unit.nva.customer.model.interfaces.Typed;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean(converterProviders = {VocabularyConverterProvider.class, DefaultAttributeConverterProvider.class})
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.TooManyFields", "PMD.CouplingBetweenObjects"})
public class CustomerDao implements Typed, JsonSerializable {

    public static final String IDENTIFIER = "identifier";
    public static final String ORG_DOMAIN = "feideOrganizationDomain";
    public static final String CRISTIN_ID = "cristinId";
    public static final String TYPE = "Customer";
    public static final Set<VocabularyDao> EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO = null;
    public static final List<ChannelClaimDao> CHANNEL_CLAIM_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO = null;
    public static final Set<PublicationInstanceTypes>
            ALLOW_FILE_UPLOAD_FOR_TYPES_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO = null;
    public static final TableSchema<CustomerDao> TABLE_SCHEMA = TableSchema.fromClass(CustomerDao.class);
    public static final String VOCABULARIES_FIELD = "vocabularies";
    public static final String ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD = "allowFileUploadForTypes";
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
    private ServiceCenterDao serviceCenter;
    private boolean nviInstitution;
    private boolean rboInstitution;
    private boolean generalSupportEnabled;
    private Instant inactiveFrom;
    private Sector sector;
    @JsonAlias("rightRetentionStrategy")
    private RightsRetentionStrategyDao rightsRetentionStrategy;
    private Set<PublicationInstanceTypes> allowFileUploadForTypes;
    private List<ChannelClaimDao> channelClaims;

    public CustomerDao() {
        vocabularies = EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
        allowFileUploadForTypes = ALLOW_FILE_UPLOAD_FOR_TYPES_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public static CustomerDao fromCustomerDto(CustomerDto dto) {
        return builder()
                .withArchiveName(dto.getArchiveName())
                .withCname(dto.getCname())
                .withCreatedDate(dto.getCreatedDate())
                .withCristinId(dto.getCristinId())
                .withCustomerOf(extractCustomerOf(dto))
                .withDisplayName(dto.getDisplayName())
                .withFeideOrganizationDomain(dto.getFeideOrganizationDomain())
                .withIdentifier(dto.getIdentifier())
                .withInstitutionDns(dto.getInstitutionDns())
                .withModifiedDate(dto.getModifiedDate())
                .withName(dto.getName())
                .withPublicationWorkflow(dto.getPublicationWorkflow())
                .withRorId(dto.getRorId())
                .withServiceCenter(dto.getServiceCenter().toDao())
                .withShortName(dto.getShortName())
                .withVocabularySettings(extractVocabularySettings(dto))
                .withDoiAgent(dto.getDoiAgent())
                .withNviInstitution(dto.isNviInstitution())
                .withRboInstitution(dto.isRboInstitution())
                .withInactiveFrom(dto.getInactiveFrom())
                .withSector(dto.getSector())
                .withRightsRetentionStrategy(dto.getRightsRetentionStrategy())
                .withAllowFileUploadForTypes(extractPublicationInstanceTypes(dto))
                .withGeneralSupportEnabled(dto.isGeneralSupportEnabled())
                .withChannelClaims(extractChannelClaims(dto))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static URI extractCustomerOf(CustomerDto dto) {
        return Optional.ofNullable(dto)
                .map(CustomerDto::getCustomerOf)
                .map(ApplicationDomain::toString)
                .map(URI::create)
                .orElse(null);
    }

    private static Set<VocabularyDao> extractVocabularySettings(CustomerDto dto) {
        return Optional.ofNullable(dto.getVocabularies())
                .stream()
                .flatMap(Collection::stream)
                .map(VocabularyDao::fromVocabularySettingsDto)
                .collect(Collectors.toSet());
    }

    private static Set<PublicationInstanceTypes> extractPublicationInstanceTypes(CustomerDto dto) {
        return Optional.ofNullable(dto.getAllowFileUploadForTypes())
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static List<ChannelClaimDao> extractChannelClaims(CustomerDto dto) {
        return Optional.ofNullable(dto.getChannelClaims())
                .stream()
                .flatMap(Collection::stream)
                .map(ChannelClaimDao::fromDto)
                .collect(Collectors.toList());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getCreatedDate(), getModifiedDate(), getName(), getDisplayName(),
                getShortName(), getArchiveName(), getCname(), getInstitutionDns(),
                getFeideOrganizationDomain(),
                getCristinId(), getCustomerOf(), getVocabularies(), getRorId(),
                getPublicationWorkflow(), getDoiAgent(), getServiceCenter(), isNviInstitution(),
                isRboInstitution(), isGeneralSupportEnabled(), getInactiveFrom(), getSector(),
                getRightsRetentionStrategy(), getAllowFileUploadForTypes(), getChannelClaims());
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
        return isNviInstitution() == that.isNviInstitution()
                && isRboInstitution() == that.isRboInstitution()
                && isGeneralSupportEnabled() == that.isGeneralSupportEnabled()
                && Objects.equals(getIdentifier(), that.getIdentifier())
                && Objects.equals(getCreatedDate(), that.getCreatedDate())
                && Objects.equals(getModifiedDate(), that.getModifiedDate())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getDisplayName(), that.getDisplayName())
                && Objects.equals(getShortName(), that.getShortName())
                && Objects.equals(getArchiveName(), that.getArchiveName())
                && Objects.equals(getCname(), that.getCname())
                && Objects.equals(getInstitutionDns(), that.getInstitutionDns())
                && Objects.equals(getFeideOrganizationDomain(), that.getFeideOrganizationDomain())
                && Objects.equals(getCristinId(), that.getCristinId())
                && Objects.equals(getCustomerOf(), that.getCustomerOf())
                && Objects.equals(getVocabularies(), that.getVocabularies())
                && Objects.equals(getRorId(), that.getRorId())
                && getPublicationWorkflow() == that.getPublicationWorkflow()
                && Objects.equals(getDoiAgent(), that.getDoiAgent())
                && Objects.equals(getServiceCenter(), that.getServiceCenter())
                && Objects.equals(getInactiveFrom(), that.getInactiveFrom())
                && getSector() == that.getSector()
                && Objects.equals(getRightsRetentionStrategy(), that.getRightsRetentionStrategy())
                && Objects.equals(getAllowFileUploadForTypes(), that.getAllowFileUploadForTypes())
                && Objects.equals(getChannelClaims(), that.getChannelClaims());
    }

    @Override
    public String toString() {
        return "CustomerDao{" +
                "identifier=" + identifier +
                ", createdDate=" + createdDate +
                ", modifiedDate=" + modifiedDate +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", shortName='" + shortName + '\'' +
                ", archiveName='" + archiveName + '\'' +
                ", cname='" + cname + '\'' +
                ", institutionDns='" + institutionDns + '\'' +
                ", feideOrganizationDomain='" + feideOrganizationDomain + '\'' +
                ", cristinId=" + cristinId +
                ", customerOf=" + customerOf +
                ", vocabularies=" + vocabularies +
                ", rorId=" + rorId +
                ", publicationWorkflow=" + publicationWorkflow +
                ", doiAgent=" + doiAgent +
                ", serviceCenter=" + serviceCenter +
                ", nviInstitution=" + nviInstitution +
                ", rboInstitution=" + rboInstitution +
                ", generalSupportEnabled=" + generalSupportEnabled +
                ", inactiveFrom=" + inactiveFrom +
                ", sector=" + sector +
                ", rightsRetentionStrategy=" + rightsRetentionStrategy +
                ", allowFileUploadForTypes=" + allowFileUploadForTypes +
                ", channelClaims=" + channelClaims +
                '}';
    }

    public boolean isRboInstitution() {
        return rboInstitution;
    }

    public void setRboInstitution(boolean rboInstitution) {
        this.rboInstitution = rboInstitution;
    }

    public Instant getInactiveFrom() {
        return inactiveFrom;
    }

    public void setInactiveFrom(Instant inactiveFrom) {
        this.inactiveFrom = inactiveFrom;
    }

    public boolean isGeneralSupportEnabled() {
        return generalSupportEnabled;
    }

    public void setGeneralSupportEnabled(boolean generalSupportEnabled) {
        this.generalSupportEnabled = generalSupportEnabled;
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
        if (nonNull(cristinId) && StringUtils.isNotEmpty(cristinId.toString())) {
            this.cristinId = cristinId;
        }
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
        if (nonNull(rorId) && StringUtils.isNotEmpty(rorId.toString())) {
            this.rorId = rorId;
        }
    }

    public PublicationWorkflow getPublicationWorkflow() {
        return nonNull(publicationWorkflow) ? publicationWorkflow
                : PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
    }

    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }

    @DynamoDbConvertedBy(DoiAgentConverter.class)
    public DoiAgentDao getDoiAgent() {
        return nonNull(doiAgent)
                ? doiAgent
                : new DoiAgentDao();
    }

    public void setDoiAgent(DoiAgentDao doi) {
        this.doiAgent = doi;
    }

    public boolean isNviInstitution() {
        return nviInstitution;
    }

    public void setNviInstitution(boolean nviInstitution) {
        this.nviInstitution = nviInstitution;
    }

    public Sector getSector() {
        return nonNull(sector) ? sector : Sector.UHI;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    @DynamoDbIgnoreNulls
    @DynamoDbAttribute(ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD)
    public Set<PublicationInstanceTypes> getAllowFileUploadForTypes() {
        return nonEmpty(allowFileUploadForTypes)
                ? allowFileUploadForTypes
                : ALLOW_FILE_UPLOAD_FOR_TYPES_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public void setAllowFileUploadForTypes(Set<PublicationInstanceTypes> allowFileUploadForTypes) {
        this.allowFileUploadForTypes =
                nonEmpty(allowFileUploadForTypes)
                        ? allowFileUploadForTypes
                        : ALLOW_FILE_UPLOAD_FOR_TYPES_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    @DynamoDbConvertedBy(RightsRetentionStrategyConverter.class)
    public RightsRetentionStrategyDao getRightsRetentionStrategy() {
        return nonNull(rightsRetentionStrategy)
                ? rightsRetentionStrategy
                : new RightsRetentionStrategyDao();
    }

    public void setRightsRetentionStrategy(RightsRetentionStrategyDao rightsRetentionStrategy) {
        this.rightsRetentionStrategy = rightsRetentionStrategy;
    }

    public ServiceCenterDao getServiceCenter() {
        return nonNull(serviceCenter) ? serviceCenter : new ServiceCenterDao();
    }

    public void setServiceCenter(ServiceCenterDao serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    public List<ChannelClaimDao> getChannelClaims() {
        return nonNull(channelClaims) ? channelClaims : CHANNEL_CLAIM_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public void setChannelClaims(List<ChannelClaimDao> channelClaims) {
        this.channelClaims = nonEmpty(channelClaims) ? channelClaims : CHANNEL_CLAIM_EMPTY_VALUE_ACCEPTABLE_BY_DYNAMO;
    }

    public CustomerDto toCustomerDto() {
        return CustomerDto.builder()
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
                .withServiceCenter(getServiceCenter().toDto())
                .withPublicationWorkflow(getPublicationWorkflow())
                .withDoiAgent(getDoiAgent())
                .withNviInstitution(isNviInstitution())
                .withRboInstitution(isRboInstitution())
                .withInactiveFrom(getInactiveFrom())
                .withSector(getSector())
                .withRightsRetentionStrategy(getRightsRetentionStrategy())
                .withAllowFileUploadForTypes(getAllowFileUploadForTypes())
                .withGeneralSupportEnabled(generalSupportEnabled)
                .withChannelClaims(extractChannelClaims())
                .build();
    }

    private Set<VocabularyDto> extractVocabularySettings() {
        return Optional.ofNullable(this.getVocabularies())
                .stream()
                .flatMap(Collection::stream)
                .map(VocabularyDao::toVocabularySettingsDto)
                .collect(Collectors.toSet());
    }

    private List<ChannelClaimDto> extractChannelClaims() {
        return Optional.ofNullable(this.getChannelClaims())
                .stream()
                .flatMap(Collection::stream)
                .map(ChannelClaimDao::toDto)
                .collect(Collectors.toList());
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
            customerDb.setCreatedDate(attempt(() -> Instant.parse(createdDate)).orElse((e) -> null));
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            customerDb.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withModifiedDate(String modifiedDate) {
            customerDb.setModifiedDate(attempt(() -> Instant.parse(modifiedDate)).orElse((a) -> null));
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

        public Builder withServiceCenter(ServiceCenterDao serviceCenterDao) {
            customerDb.setServiceCenter(serviceCenterDao);
            return this;
        }

        public Builder withPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
            customerDb.setPublicationWorkflow(publicationWorkflow);
            return this;
        }

        @SuppressWarnings({"PMD.NullAssignment"})
        public Builder withDoiAgent(DoiAgent doiAgent) {
            customerDb.setDoiAgent(nonNull(doiAgent) ? new DoiAgentDao(doiAgent) : null);
            return this;
        }

        public Builder withNviInstitution(boolean hasNviInstitution) {
            customerDb.setNviInstitution(hasNviInstitution);
            return this;
        }

        public Builder withRightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
            customerDb.setRightsRetentionStrategy(new RightsRetentionStrategyDao(rightsRetentionStrategy));
            return this;
        }

        public Builder withSector(Sector sector) {
            customerDb.setSector(sector);
            return this;
        }

        public Builder withAllowFileUploadForTypes(Set<PublicationInstanceTypes> allowFileUploadForTypes) {
            customerDb.setAllowFileUploadForTypes(allowFileUploadForTypes);
            return this;
        }

        public Builder withRboInstitution(boolean rboInstitution) {
            customerDb.setRboInstitution(rboInstitution);
            return this;
        }

        public Builder withInactiveFrom(Instant inactiveFrom) {
            customerDb.setInactiveFrom(inactiveFrom);
            return this;
        }

        public Builder withGeneralSupportEnabled(boolean generalSupportEnabled) {
            customerDb.setGeneralSupportEnabled(generalSupportEnabled);
            return this;
        }

        public Builder withChannelClaims(List<ChannelClaimDao> channelClaimDao) {
            customerDb.setChannelClaims(channelClaimDao);
            return this;
        }

        public CustomerDao build() {
            return customerDb;
        }
    }

    @DynamoDbBean
    public static class DoiAgentDao implements DoiAgent, JsonSerializable {

        private String prefix;
        private String url;
        private String username;

        public DoiAgentDao() {
        }

        public DoiAgentDao(DoiAgent doiAgent) {
            this.prefix = doiAgent.getPrefix();
            this.url = doiAgent.getUrl();
            this.username = doiAgent.getUsername();
        }

        public DoiAgentDto toDoiAgentDto() {
            return new DoiAgentDto(this);
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(getPrefix(), getUrl(), getUsername());
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        @JacocoGenerated
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DoiAgentDao that)) {
                return false;
            }
            return Objects.equals(getPrefix(), that.getPrefix())
                    && Objects.equals(getUrl(), that.getUrl())
                    && Objects.equals(getUsername(), that.getUsername());
        }

        @Override
        @JacocoGenerated
        public String toString() {
            return toJsonString();
        }
    }

    @DynamoDbBean
    public static class RightsRetentionStrategyDao implements RightsRetentionStrategy, JsonSerializable {

        @JsonAlias("retentionStrategy")
        private RightsRetentionStrategyType type;
        private URI id;

        public RightsRetentionStrategyDao() {
            type = RightsRetentionStrategyType.NullRightsRetentionStrategy;
        }

        public RightsRetentionStrategyDao(RightsRetentionStrategyType type, URI id) {
            this.type = type;
            this.id = id;
        }

        public RightsRetentionStrategyDao(RightsRetentionStrategy rightsRetentionStrategy) {
            if (nonNull(rightsRetentionStrategy)) {
                this.type = rightsRetentionStrategy.getType();
                this.id = rightsRetentionStrategy.getId();
            }
        }

        @Override
        public RightsRetentionStrategyType getType() {
            return type;
        }

        public void setType(RightsRetentionStrategyType rightsRetentionStrategyType) {
            this.type = rightsRetentionStrategyType;
        }

        @Override
        public URI getId() {
            return id;
        }

        public void setId(URI id) {
            this.id = id;
        }

        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(type, id);
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
            RightsRetentionStrategyDao that = (RightsRetentionStrategyDao) o;
            return type == that.type && Objects.equals(id, that.id);
        }

        @Override
        @JacocoGenerated
        public String toString() {
            return toJsonString();
        }
    }

    @DynamoDbBean
    public static class ServiceCenterDao implements JsonSerializable {

        private URI uri;
        private String name;

        @JacocoGenerated
        public ServiceCenterDao() {
        }

        public ServiceCenterDao(URI uri, String name) {
            this.uri = uri;
            this.name = name;
        }

        public ServiceCenter toDto() {
            return new ServiceCenter(uri, name);
        }

        @JacocoGenerated
        @Override
        public int hashCode() {
            return Objects.hash(getUri(), getName());
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
            ServiceCenterDao that = (ServiceCenterDao) o;
            return Objects.equals(getUri(), that.getUri()) && Objects.equals(getName(), that.getName());
        }

        @JacocoGenerated
        @Override
        public String toString() {
            return this.toJsonString();
        }

        public URI getUri() {
            return uri;
        }

        @JacocoGenerated
        public void setUri(URI uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        @JacocoGenerated
        public void setName(String name) {
            this.name = name;
        }
    }
}