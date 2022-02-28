package no.unit.nva.customer.service.impl;

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
import no.unit.nva.customer.exception.DynamoDBException;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.exception.NotFoundException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nva.commons.core.attempt.Try.attempt;

public class DynamoDBCustomerService implements CustomerService {

    public static final String CUSTOMERS_TABLE_NAME = "CUSTOMERS_TABLE_NAME";
    public static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    public static final String ERROR_MAPPING_ITEM_TO_CUSTOMER = "Error mapping Item to Customer";
    public static final String ERROR_MAPPING_CUSTOMER_TO_ITEM = "Error mapping Customer to Item";
    public static final String ERROR_WRITING_ITEM_TO_TABLE = "Error writing Item to Table";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    public static final String ERROR_READING_FROM_TABLE = "Error reading from Table";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";
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
        String tableName = environment.readEnv(CUSTOMERS_TABLE_NAME);
        DynamoDB dynamoDB = new DynamoDB(client);

        this.table = dynamoDB.getTable(tableName);
        this.byOrgNumberIndex = table.getIndex(BY_ORG_NUMBER_INDEX_NAME);
        this.byCristinIdIndex = table.getIndex(BY_CRISTIN_ID_INDEX_NAME);
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
    public CustomerDao getCustomer(UUID identifier) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(table, CustomerDao.IDENTIFIER, identifier.toString());
        return itemToCustomer(item);
    }

    @Override
    public CustomerDao getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(byOrgNumberIndex, CustomerDao.ORG_NUMBER, orgNumber);
        return itemToCustomer(item);
    }

    @Override
    public List<CustomerDao> getCustomers() throws ApiGatewayException {
        ItemCollection<ScanOutcome> scan;
        try {
            scan = table.scan();
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_READING_FROM_TABLE, e);
        }
        return scanToCustomers(scan);
    }

    @Override
    public CustomerDao createCustomer(CustomerDao customer) throws ApiGatewayException {
        try {
            CustomerDao customerCopy = customer.copy();
            customerCopy.setModifiedDate(Instant.now());
            table.putItem(customerToItem(customerCopy));
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_WRITING_ITEM_TO_TABLE, e);
        }
        return getCustomer(customer.getIdentifier());
    }

    @Override
    public CustomerDao updateCustomer(UUID identifier, CustomerDao customer) throws ApiGatewayException {
        validateIdentifier(identifier, customer);
        try {
            CustomerDao customerCopy = customer.copy();
            customerCopy.setModifiedDate(Instant.now());
            Item item = customerToItem(customerCopy);
            table.putItem(item);
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_WRITING_ITEM_TO_TABLE, e);
        }
        return getCustomer(identifier);
    }

    @Override
    public CustomerDao getCustomerByCristinId(String cristinId) throws ApiGatewayException {
        Item item = fetchItemFromQueryable(byCristinIdIndex, CustomerDao.CRISTIN_ID, cristinId);
        return itemToCustomer(item);
    }

    protected List<CustomerDao> scanToCustomers(ItemCollection<ScanOutcome> scan) throws DynamoDBException {
        List<CustomerDao> customers = new ArrayList<>();
        for (Item item : scan) {
            customers.add(itemToCustomer(item));
        }
        return customers;
    }

    protected Item customerToItem(CustomerDao customer) throws InputException {
        Item item;
        try {
            item = Item.fromJSON(objectMapper.writeValueAsString(customer));
        } catch (JsonProcessingException e) {
            throw new InputException(ERROR_MAPPING_CUSTOMER_TO_ITEM, e);
        }
        return item;
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    protected CustomerDao itemToCustomer(Item item) throws DynamoDBException {
        long start = System.currentTimeMillis();
        CustomerDao customerOutcome;
        try {
            customerOutcome = objectMapper.readValue(item.toJSON(), CustomerDao.class);
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_MAPPING_ITEM_TO_CUSTOMER, e);
        }
        long stop = System.currentTimeMillis();
        logger.info("itemToCustomer took {} ms", stop - start);
        return customerOutcome;
    }

    private void warmupDynamoDbConnection(Table table) {
        try {
            table.describe();
        } catch (Exception e) {
            logger.warn(DYNAMODB_WARMUP_PROBLEM, e);
        }
    }

    private void validateIdentifier(UUID identifier, CustomerDao customer) throws InputException {
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
