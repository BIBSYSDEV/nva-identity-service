package no.unit.nva.customer.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.api.QueryApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.customer.exception.DynamoDBException;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.exception.NotFoundException;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBCustomerService implements CustomerService {

    public static final String TABLE_NAME = "TABLE_NAME";
    public static final String BY_ORG_NUMBER_INDEX_NAME = "BY_ORG_NUMBER_INDEX_NAME";
    public static final String ERROR_MAPPING_ITEM_TO_CUSTOMER = "Error mapping Item to Customer";
    public static final String ERROR_MAPPING_CUSTOMER_TO_ITEM = "Error mapping Customer to Item";
    public static final String ERROR_WRITING_ITEM_TO_TABLE = "Error writing Item to Table";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    public static final String ERROR_READING_FROM_TABLE = "Error reading from Table";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "BY_CRISTIN_ID_INDEX_NAME";
    public static final String DYNAMODB_WARMUP_PROBLEM = "There was a problem during describe table to warm up "
                                                         + "DynamoDB connection";
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCustomerService.class);
    private final Table table;
    private final Index byOrgNumberIndex;
    private final Index byCristinIdIndex;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for DynamoDBCustomerService.
     *
     * @param client       AmazonDynamoDB client
     * @param objectMapper Jackson objectMapper
     * @param environment  Environment reader
     */
    public DynamoDBCustomerService(AmazonDynamoDB client,
                                   ObjectMapper objectMapper,
                                   Environment environment) {
        String tableName = environment.readEnv(TABLE_NAME);
        String byOrgNumberIndexName = environment.readEnv(BY_ORG_NUMBER_INDEX_NAME);
        String byCristinIdIndexName = environment.readEnv(BY_CRISTIN_ID_INDEX_NAME);
        DynamoDB dynamoDB = new DynamoDB(client);

        this.table = dynamoDB.getTable(tableName);
        this.byOrgNumberIndex = table.getIndex(byOrgNumberIndexName);
        this.byCristinIdIndex = table.getIndex(byCristinIdIndexName);
        this.objectMapper = objectMapper;

        warmupDynamoDbConnection(table);
    }

    /**
     * Constructor for DynamoDBCustomerService.
     *
     * @param objectMapper     Jackson objectMapper
     * @param table            table name
     * @param byOrgNumberIndex index name
     */
    public DynamoDBCustomerService(ObjectMapper objectMapper,
                                   Table table,
                                   Index byOrgNumberIndex,
                                   Index byCristinIdIndex) {
        this.table = table;
        this.byOrgNumberIndex = byOrgNumberIndex;
        this.byCristinIdIndex = byCristinIdIndex;
        this.objectMapper = objectMapper;

        warmupDynamoDbConnection(table);
    }

    @Override
    public CustomerDto getCustomer(UUID identifier) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(table, CustomerDb.IDENTIFIER, identifier.toString());
        return itemToCustomer(item);
    }

    @Override
    public CustomerDto getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(byOrgNumberIndex, CustomerDb.ORG_NUMBER, orgNumber);
        return itemToCustomer(item);
    }

    @Override
    public List<CustomerDto> getCustomers() throws ApiGatewayException {
        ItemCollection<ScanOutcome> scan;
        try {
            scan = table.scan();
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_READING_FROM_TABLE, e);
        }
        return scanToCustomers(scan);
    }

    @Override
    public CustomerDto createCustomer(CustomerDto customer) throws ApiGatewayException {
        UUID identifier = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            customer.setIdentifier(identifier);
            customer.setCreatedDate(now);
            customer.setModifiedDate(now);
            table.putItem(customerToItem(customer));
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_WRITING_ITEM_TO_TABLE, e);
        }
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto updateCustomer(UUID identifier, CustomerDto customer) throws ApiGatewayException {
        validateIdentifier(identifier, customer);
        try {
            customer.setModifiedDate(Instant.now());
            Item item = customerToItem(customer);
            table.putItem(item);
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_WRITING_ITEM_TO_TABLE, e);
        }
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto getCustomerByCristinId(String cristinId) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(byCristinIdIndex, CustomerDb.CRISTIN_ID, cristinId);
        return itemToCustomer(item);
    }

    protected List<CustomerDto> scanToCustomers(ItemCollection<ScanOutcome> scan) throws DynamoDBException {
        List<CustomerDto> customers = new ArrayList<>();
        for (Item item : scan) {
            customers.add(itemToCustomer(item));
        }
        return customers;
    }

    protected Item customerToItem(CustomerDto customer) throws InputException {
        Item item;
        try {
            CustomerDb dao = CustomerDb.fromCustomerDto(customer);
            item = Item.fromJSON(objectMapper.writeValueAsString(dao));
        } catch (JsonProcessingException e) {
            throw new InputException(ERROR_MAPPING_CUSTOMER_TO_ITEM, e);
        }
        return item;
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    protected CustomerDto itemToCustomer(Item item) throws DynamoDBException {
        long start = System.currentTimeMillis();
        CustomerDb customerOutcome;
        try {
            customerOutcome = objectMapper.readValue(item.toJSON(), CustomerDb.class);
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_MAPPING_ITEM_TO_CUSTOMER, e);
        }
        long stop = System.currentTimeMillis();
        logger.info("itemToCustomer took {} ms", stop - start);
        return customerOutcome.toCustomerDto();
    }

    private void warmupDynamoDbConnection(Table table) {
        try {
            table.describe();
        } catch (Exception e) {
            logger.warn(DYNAMODB_WARMUP_PROBLEM, e);
        }
    }

    private void validateIdentifier(UUID identifier, CustomerDto customer) throws InputException {
        if (!identifier.equals(customer.getIdentifier())) {
            throw new InputException(String.format(IDENTIFIERS_NOT_EQUAL, identifier, customer.getIdentifier()), null);
        }
    }

    private Item fetchItemFromQueryable(QueryApi index, String hashKeyName, String hashKeyValue)
        throws DynamoDBException, NotFoundException {
        long start = System.currentTimeMillis();
        Optional<Item> queryResult = attempt(() -> index.query(hashKeyName, hashKeyValue))
            .map(this::fetchSingleItem)
            .orElseThrow(fail -> new DynamoDBException(ERROR_READING_FROM_TABLE, fail.getException()));
        long stop = System.currentTimeMillis();
        logger.info("fetchItemFromQueryable took {} ms", stop - start);
        return queryResult.orElseThrow(() -> notFoundException(hashKeyValue));
    }

    private Optional<Item> fetchSingleItem(ItemCollection<QueryOutcome> query) {
        Iterator<Item> iterator = query.iterator();
        if (iterator.hasNext()) {
            return Optional.of(iterator.next());
        }
        return Optional.empty();
    }

    private NotFoundException notFoundException(String queryValue) {
        return new NotFoundException(CUSTOMER_NOT_FOUND + queryValue);
    }
}
