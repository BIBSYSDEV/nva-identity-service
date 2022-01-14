package no.unit.nva.customer.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.exception.DynamoDBException;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDBCustomerService implements CustomerService {

    public static final String ERROR_MAPPING_ITEM_TO_CUSTOMER = "Error mapping Item to Customer";
    public static final String ERROR_MAPPING_CUSTOMER_TO_ITEM = "Error mapping Customer to Item";
    public static final String ERROR_WRITING_ITEM_TO_TABLE = "Error writing Item to Table";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    public static final String ERROR_READING_FROM_TABLE = "Error reading from Table";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String DYNAMODB_WARMUP_PROBLEM = "There was a problem during describe table to warm up "
                                                         + "DynamoDB connection";
    private static final Environment ENVIRONMENT = new Environment();
    public static final String TABLE_NAME = ENVIRONMENT.readEnv("TABLE_NAME");
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";
    public static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCustomerService.class);
    private final DynamoDbTable<CustomerDao> table;

    /**
     * Constructor for DynamoDBCustomerService.
     *
     * @param client AmazonDynamoDB client
     */
    public DynamoDBCustomerService(DynamoDbClient client) {
        this(createTable(client));
    }

    public DynamoDBCustomerService(DynamoDbTable<CustomerDao> table) {
        this.table = table;
        warmupDynamoDbConnection(table);
    }

    @Override
    public CustomerDto getCustomer(UUID identifier) throws ApiGatewayException {
        return Optional.of(CustomerDao.builder().withIdentifier(identifier).build())
            .map(table::getItem)
            .map(CustomerDao::toCustomerDto)
            .orElseThrow(() -> notFoundException(identifier.toString()));
    }

    @Override
    public CustomerDto getCustomerByOrgNumber(String orgNumber) throws ApiGatewayException {
        CustomerDao query = createQueryForOrgNumber(orgNumber);
        return sendQueryToIndex(orgNumber, query, BY_ORG_NUMBER_INDEX_NAME);
    }

    @Override
    public List<CustomerDto> getCustomers() {
        return table.scan()
            .stream()
            .flatMap(page -> page.items().stream())
            .map(CustomerDao::toCustomerDto)
            .collect(Collectors.toList());
    }

    @Override
    public CustomerDto createCustomer(CustomerDto customer) throws ApiGatewayException {
        UUID identifier = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            customer.setIdentifier(identifier);
            customer.setCreatedDate(now);
            customer.setModifiedDate(now);
            table.putItem(CustomerDao.fromCustomerDto(customer));
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
            table.putItem(CustomerDao.fromCustomerDto(customer));
        } catch (Exception e) {
            throw new DynamoDBException(ERROR_WRITING_ITEM_TO_TABLE, e);
        }
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto getCustomerByCristinId(String cristinId) throws ApiGatewayException {
        CustomerDao queryObject = createQueryForCristinNumber(cristinId);
        return sendQueryToIndex(cristinId, queryObject, BY_CRISTIN_ID_INDEX_NAME);
    }

    private static DynamoDbTable<CustomerDao> createTable(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        return enhancedClient.table(TABLE_NAME, CustomerDao.TABLE_SCHEMA);
    }

    private CustomerDto sendQueryToIndex(String queryValue, CustomerDao queryObject, String indexName)
        throws NotFoundException {
        QueryEnhancedRequest query = createQuery(queryObject);
        return table.index(indexName)
            .query(query)
            .stream()
            .flatMap(page -> page.items().stream())
            .map(CustomerDao::toCustomerDto)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> notFoundException(queryValue));
    }

    private CustomerDao createQueryForOrgNumber(String orgNumber) {
        return CustomerDao.builder().withFeideOrganizationId(orgNumber).build();
    }

    private CustomerDao createQueryForCristinNumber(String cristinId) {
        return CustomerDao.builder().withCristinId(cristinId).build();
    }

    private QueryEnhancedRequest createQuery(CustomerDao queryObject) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder().partitionValue(queryObject.getFeideOrganizationId()).build());
        return QueryEnhancedRequest
            .builder()
            .queryConditional(queryConditional)
            .build();
    }

    private void warmupDynamoDbConnection(DynamoDbTable<CustomerDao> table) {
        try {
            table.describeTable();
        } catch (Exception e) {
            logger.warn(DYNAMODB_WARMUP_PROBLEM, e);
        }
    }

    private void validateIdentifier(UUID identifier, CustomerDto customer) throws InputException {
        if (!identifier.equals(customer.getIdentifier())) {
            throw new InputException(String.format(IDENTIFIERS_NOT_EQUAL, identifier, customer.getIdentifier()), null);
        }
    }

    private NotFoundException notFoundException(String queryValue) {
        return new NotFoundException(CUSTOMER_NOT_FOUND + queryValue);
    }
}
