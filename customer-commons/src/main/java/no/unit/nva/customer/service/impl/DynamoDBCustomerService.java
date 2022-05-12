package no.unit.nva.customer.service.impl;

import static nva.commons.core.attempt.Try.attempt;
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
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
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

    public static final String BY_ORG_DOMAIN_INDEX_NAME = "byOrgDomain";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";

    public static final String DYNAMODB_WARMUP_PROBLEM = "There was a problem during describe table to warm up "
                                                         + "DynamoDB connection";
    public static final String CUSTOMER_ALREADY_EXISTS_ERROR = "Customer with Institution ID %s already exists.";
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
    public CustomerDto getCustomer(URI customerId) throws NotFoundException {
        var customerIdentifier = UriWrapper.fromUri(customerId).getLastPathElement();
        return getCustomer(UUID.fromString(customerIdentifier));
    }

    @Override
    public CustomerDto getCustomer(UUID identifier) throws NotFoundException {
        return Optional.of(CustomerDao.builder().withIdentifier(identifier).build())
            .map(table::getItem)
            .map(CustomerDao::toCustomerDto)
            .orElseThrow(() -> notFoundException(identifier.toString()));
    }

    @Override
    public CustomerDto getCustomerByOrgDomain(String orgDomain) throws NotFoundException {
        CustomerDao query = createQueryForOrgDomain(orgDomain);
        return sendQueryToIndex(query, BY_ORG_DOMAIN_INDEX_NAME, CustomerDao::getFeideOrganizationDomain);
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
    public CustomerDto createCustomer(CustomerDto customer) throws NotFoundException, ConflictException {
        checkForConflict(customer.getCristinId());
        var newCustomer = addInternalDetails(customer);
        table.putItem(CustomerDao.fromCustomerDto(newCustomer));
        return getCustomer(newCustomer.getIdentifier());
    }

    @Override
    public CustomerDto updateCustomer(UUID identifier, CustomerDto customer) throws InputException, NotFoundException {
        validateIdentifier(identifier, customer);
        customer.setModifiedDate(Instant.now().toString());
        table.putItem(CustomerDao.fromCustomerDto(customer));
        return getCustomer(identifier);
    }

    @Override
    public CustomerDto getCustomerByCristinId(URI cristinId) throws NotFoundException {
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

    private CustomerDto addInternalDetails(CustomerDto customer) {
        Instant now = Instant.now();
        return customer.copy()
            .withIdentifier(UUID.randomUUID())
            .withCreatedDate(now)
            .withModifiedDate(now)
            .build();
    }

    private void checkForConflict(URI institutionId) throws ConflictException {
        var customerExists = attempt(() -> getCustomerByCristinId(institutionId)).toOptional().isPresent();
        if (customerExists) {
            throw new ConflictException(String.format(CUSTOMER_ALREADY_EXISTS_ERROR, institutionId.toString()));
        }
    }

    private CustomerDto sendQueryToIndex(CustomerDao queryObject,
                                         String indexName,
                                         Function<CustomerDao, String> indexPartitionValue) throws NotFoundException {
        QueryEnhancedRequest query = createQuery(queryObject, indexPartitionValue);
        var results = table.index(indexName).query(query);
        return results
            .stream()
            .flatMap(page -> page.items().stream())
            .map(CustomerDao::toCustomerDto)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(fail -> notFoundException(queryObject.toString()));
    }

    private CustomerDao createQueryForOrgDomain(String feideDomain) {
        return CustomerDao.builder().withFeideOrganizationDomain(feideDomain).build();
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

    private void validateIdentifier(UUID identifier, CustomerDto customer) throws InputException {
        if (!identifier.equals(customer.getIdentifier())) {
            throw new InputException(String.format(IDENTIFIERS_NOT_EQUAL, identifier, customer.getIdentifier()), null);
        }
    }

    private NotFoundException notFoundException(String queryValue) {
        return new NotFoundException(CUSTOMER_NOT_FOUND + queryValue);
    }
}
