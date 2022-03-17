package no.unit.nva.customer.service.impl;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDBCustomerService implements CustomerService {

    public static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";

    public static final String DYNAMODB_WARMUP_PROBLEM = "There was a problem during describe table to warm up "
                                                         + "DynamoDB connection";
    private static final Environment ENVIRONMENT = new Environment();
    public static final String CUSTOMERS_TABLE_NAME = ENVIRONMENT.readEnv("CUSTOMERS_TABLE_NAME");
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
    public CustomerDto getCustomer(URI customerId) {
        var customerIdentifier = UriWrapper.fromUri(customerId).getLastPathElement();
        return getCustomer(UUID.fromString(customerIdentifier));
    }

    @Override
    public CustomerDto getCustomer(UUID identifier) {
        return Optional.of(CustomerDao.builder().withIdentifier(identifier).build())
            .map(table::getItem)
            .map(CustomerDao::toCustomerDto)
            .orElseThrow(() -> notFoundException(identifier.toString()));
    }

    @Override
    public CustomerDto getCustomerByOrgNumber(String orgNumber) {
        CustomerDao query = createQueryForOrgNumber(orgNumber);
        return sendQueryToIndex(query, BY_ORG_NUMBER_INDEX_NAME, CustomerDao::getFeideOrganizationDomain);
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
    public CustomerDto createCustomer(CustomerDto customer) {
        UUID identifier = UUID.randomUUID();
        Instant now = Instant.now();
        customer.setIdentifier(identifier);
        customer.setCreatedDate(now.toString());
        customer.setModifiedDate(now.toString());
        table.putItem(CustomerDao.fromCustomerDto(customer));
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto updateCustomer(UUID identifier, CustomerDto customer) {
        validateIdentifier(identifier, customer);
        customer.setModifiedDate(Instant.now().toString());
        table.putItem(CustomerDao.fromCustomerDto(customer));
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto getCustomerByCristinId(URI cristinId) {
        CustomerDao queryObject = createQueryForCristinNumber(cristinId);
        return sendQueryToIndex(queryObject, BY_CRISTIN_ID_INDEX_NAME,
                                customer -> customer.getCristinId().toString());
    }

    private static DynamoDbTable<CustomerDao> createTable(DynamoDbClient client) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build();
        return enhancedClient.table(CUSTOMERS_TABLE_NAME, CustomerDao.TABLE_SCHEMA);
    }

    private CustomerDto sendQueryToIndex(CustomerDao queryObject,
                                         String indexName,
                                         Function<CustomerDao, String> indexPartitionValue) {
        QueryEnhancedRequest query = createQuery(queryObject, indexPartitionValue);
        var results = table.index(indexName).query(query);
        return results
            .stream()
            .flatMap(page -> page.items().stream())
            .map(CustomerDao::toCustomerDto)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> notFoundException(queryObject.toString()));
    }

    private CustomerDao createQueryForOrgNumber(String orgNumber) {
        return CustomerDao.builder().withFeideOrganizationId(orgNumber).build();
    }

    private CustomerDao createQueryForCristinNumber(URI cristinId) {
        return CustomerDao.builder().withCristinId(cristinId).build();
    }

    private QueryEnhancedRequest createQuery(CustomerDao queryObject,
                                             Function<CustomerDao, String> indexPartitionValue) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder().partitionValue(indexPartitionValue.apply(queryObject)).build());
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

    private void validateIdentifier(UUID identifier, CustomerDto customer) {
        if (!identifier.equals(customer.getIdentifier())) {
            throw new InputException(String.format(IDENTIFIERS_NOT_EQUAL, identifier, customer.getIdentifier()), null);
        }
    }

    private NotFoundException notFoundException(String queryValue) {
        return new NotFoundException(CUSTOMER_NOT_FOUND + queryValue);
    }
}
