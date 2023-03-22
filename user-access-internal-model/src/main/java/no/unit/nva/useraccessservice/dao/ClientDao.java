package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SEARCH_USERS_BY_CRISTIN_IDENTIFIERS;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_2_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_2_RANGE_KEY;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.useraccessservice.dao.ClientDao.Builder;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.ClientDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
@DynamoDbBean(converterProviders = {RoleSetConverterProvider.class, DefaultAttributeConverterProvider.class})
public class ClientDao implements DynamoEntryWithRangeKey, WithCopy<Builder> {

    public static final TableSchema<ClientDao> TABLE_SCHEMA = TableSchema.fromClass(ClientDao.class);
    public static final String TYPE_VALUE = "CLIENT";
    public static final String CLIENT_ID_FIELD = "client";
    public static final String CUSTOMER_FIELD = "customer";
    public static final String CRISTIN_FIELD = "cristin";
    public static final String OWNER_FIELD = "owner";

    private String clientTd;
    private URI customer;
    private URI cristin;
    private String owner;

    public ClientDao() {
        super();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static ClientDao fromClientDto(ClientDto clientDto) {
        ClientDao.Builder clientDb = ClientDao.newBuilder()
                                         .withClientId(clientDto.getClientId())
                                         .withCustomer(clientDto.getCustomer())
                                         .withCristin(clientDto.getCristin())
                                         .withOwner(clientDto.getOwner());

        return clientDb.build();
    }

    @JacocoGenerated
    @DynamoDbAttribute(CLIENT_ID_FIELD)
    public String getClientTd() {
        return clientTd;
    }

    /**
     * @param clientTd Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     */
    public void setClientTd(String clientTd) {
        this.clientTd = clientTd;
    }

    @JacocoGenerated
    @DynamoDbAttribute(CUSTOMER_FIELD)
    public URI getCustomer() {
        return customer;
    }

    /**
     * @param customer Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     */
    public void setCustomer(URI customer) {
        this.customer = customer;
    }

    /**
     * Creates a {@link ClientDto} from a {@link ClientDao}.
     *
     * @return a data transfer object {@link ClientDto}
     */

    @JacocoGenerated
    @DynamoDbAttribute(CRISTIN_FIELD)
    public URI getCristin() {
        return cristin;
    }

    /**
     * @param cristin Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     */
    public void setCristin(URI cristin) {
        this.cristin = cristin;
    }

    /**
     * Creates a {@link ClientDto} from a {@link ClientDao}.
     *
     * @return a data transfer object {@link ClientDto}
     */

    @JacocoGenerated
    @DynamoDbAttribute(OWNER_FIELD)
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Creates a {@link ClientDto} from a {@link ClientDao}.
     *
     * @return a data transfer object {@link ClientDto}
     */
    public ClientDto toClientDto() {

        ClientDto.Builder clientDto = ClientDto.newBuilder()
                                          .withClientId(this.getClientTd())
                                          .withCustomer(this.getCustomer())
                                          .withCristin(this.getCristin())
                                          .withOwner(this.getOwner());

        return clientDto.build();
    }

    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return primaryHashKeyIsTypeAndUsername();
    }

    @Override
    public void setPrimaryKeyHashKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    @DynamoDbSortKey
    @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
    public String getPrimaryKeyRangeKey() {
        return formatPrimaryRangeKey();
    }

    @Override
    @JacocoGenerated
    public void setPrimaryKeyRangeKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbSecondaryPartitionKey(indexNames = {SEARCH_USERS_BY_CRISTIN_IDENTIFIERS})
    @DynamoDbAttribute(SECONDARY_INDEX_2_HASH_KEY)
    public String getSearchByCristinIdentifiersHashKey() {
        return Optional.ofNullable(getCustomer()).map(URI::toString).orElse(null);
    }

    @JacocoGenerated
    public void setSearchByCristinIdentifiersHashKey(String searchByInstitutionHashKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbSecondarySortKey(indexNames = {SEARCH_USERS_BY_CRISTIN_IDENTIFIERS})
    @DynamoDbAttribute(SECONDARY_INDEX_2_RANGE_KEY)
    public String getSearchByCristinIdentifiersRangeKey() {
        return getClientTd();
    }

    @JacocoGenerated
    public void setSearchByCristinIdentifiersRangeKey(String searchByInstitutionRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    @DynamoDbAttribute(TYPE_FIELD)
    public String getType() {
        return TYPE_VALUE;
    }

    @Override
    @JacocoGenerated
    public void setType(String type) throws BadRequestException {
        DynamoEntryWithRangeKey.super.setType(type);
    }

    @Override
    public ClientDao.Builder copy() {
        return newBuilder()
                   .withClientId(this.getClientTd())
                   .withCustomer(this.getCustomer())
                   .withCristin(this.getCristin())
                   .withOwner(this.getOwner());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getClientTd(), getCustomer());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientDao)) {
            return false;
        }
        ClientDao clientDao = (ClientDao) o;
        return Objects.equals(getClientTd(), clientDao.getClientTd())
               && Objects.equals(getCustomer(), clientDao.getCustomer())
               && Objects.equals(getCristin(), clientDao.getCristin())
               && Objects.equals(getOwner(), clientDao.getOwner());
    }

    /*For now the primary range key does not need to be different from the primary hash key*/
    private String formatPrimaryRangeKey() {
        return primaryHashKeyIsTypeAndUsername();
    }

    private String primaryHashKeyIsTypeAndUsername() {
        return String.join(DynamoEntryWithRangeKey.FIELD_DELIMITER, TYPE_VALUE, clientTd);
    }

    public static final class Builder {

        private final ClientDao clientDao;

        private Builder() {
            this.clientDao = new ClientDao();
        }

        public Builder withClientId(String clientId) {
            clientDao.setClientTd(clientId);
            return this;
        }

        public Builder withCustomer(URI customer) {
            clientDao.setCustomer(customer);
            return this;
        }

        public Builder withCristin(URI cristin) {
            clientDao.setCristin(cristin);
            return this;
        }

        public Builder withOwner(String owner) {
            clientDao.setOwner(owner);
            return this;
        }

        public ClientDao build() {
            return clientDao;
        }
    }
}
