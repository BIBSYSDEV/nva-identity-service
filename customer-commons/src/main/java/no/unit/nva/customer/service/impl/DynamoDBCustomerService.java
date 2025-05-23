package no.unit.nva.customer.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimWithClaimer;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.validator.ChannelClaimValidator;
import nva.commons.apigateway.exceptions.BadRequestException;
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
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
                                                       + "is not equal to identifier in customer object '%s'";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";
    private static final String CUSTOMER_NOT_FOUND = "Customer not found: ";
    private static final String DYNAMODB_WARMUP_PROBLEM = "There was a problem during describe table to warm up "
                                                          + "DynamoDB connection";
    private static final String CUSTOMER_ALREADY_EXISTS_ERROR = "Customer with Institution ID %s already exists.";
    private static final Environment ENVIRONMENT = new Environment();
    public static final String CUSTOMERS_TABLE_NAME = ENVIRONMENT.readEnv("CUSTOMERS_TABLE_NAME");
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCustomerService.class);
    private static final String CHANNEL_ALREADY_CLAIMED_MESSAGE = "Channel is already claimed";
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
        return putCustomer(identifier, customer, true);
    }

    public CustomerDto putCustomer(UUID identifier, CustomerDto customer, boolean shouldOverwriteChannelClaims)
        throws NotFoundException, InputException {
        validateIdentifier(identifier, customer);
        customer.setModifiedDate(Instant.now().toString());
        if (shouldOverwriteChannelClaims) {
            var customerWithOverWrittenChannelClaims = overwriteChannelClaims(customer);
            table.putItem(CustomerDao.fromCustomerDto(customerWithOverWrittenChannelClaims));
        } else {
            table.putItem(CustomerDao.fromCustomerDto(customer));
        }
        return getCustomer(identifier);
    }

    private CustomerDto overwriteChannelClaims(CustomerDto customer) throws NotFoundException {
        var existingChannelClaims = getCustomer(customer.getIdentifier()).getChannelClaims();
        return customer.overwriteChannelClaims(existingChannelClaims);
    }

    @Override
    public CustomerDto getCustomerByCristinId(URI cristinId) throws NotFoundException {
        CustomerDao queryObject = createQueryForCristinNumber(cristinId);
        return sendQueryToIndex(queryObject, BY_CRISTIN_ID_INDEX_NAME, customer -> customer.getCristinId().toString());
    }

    @Override
    public List<CustomerDto> refreshCustomers() {
        return table.scan().items().stream()
                   .map(CustomerDao::toCustomerDto)
                   .map(this::refreshCustomer)
                   .collect(Collectors.toList());
    }

    @Override
    public void createChannelClaim(UUID customerIdentifier, ChannelClaimDto channelClaim)
        throws NotFoundException, InputException, BadRequestException, ConflictException {
        ChannelClaimValidator.validate(channelClaim);
        throwIfChannelIsAlreadyClaimed(channelClaim);
        var customer = getCustomer(customerIdentifier);
        putCustomer(customerIdentifier, customer.addChannelClaim(channelClaim), false);
    }

    private void throwIfChannelIsAlreadyClaimed(ChannelClaimDto channelClaim) throws ConflictException {
        if (getChannelClaim(channelClaim.identifier()).isPresent()) {
            throw new ConflictException(CHANNEL_ALREADY_CLAIMED_MESSAGE);
        }
    }

    @Override
    public Collection<ChannelClaimWithClaimer> getChannelClaims() {
        return getCustomers().stream()
                   .flatMap(DynamoDBCustomerService::toChannelClaimWithClaimer)
                   .toList();
    }

    @Override
    public Optional<ChannelClaimWithClaimer> getChannelClaim(UUID identifier) {
        return getCustomers().stream()
                   .flatMap(DynamoDBCustomerService::toChannelClaimWithClaimer)
                   .filter(channelClaimWithClaimer -> identifier.equals(channelClaimWithClaimer.channelClaimIdentifier()))
                   .findFirst();
    }

    @Override
    public void deleteChannelClaim(UUID identifier) throws NotFoundException, InputException {
        var channelClaimWithClaimer = getChannelClaim(identifier);
        if (channelClaimWithClaimer.isPresent()) {
            var customer = getCustomer(channelClaimWithClaimer.get().customerId());
            var updatedCustomer = customer.unclaimChannel(channelClaimWithClaimer.get().channelClaim());
            putCustomer(customer.getIdentifier(), updatedCustomer, false);
        }
    }

    private static Stream<ChannelClaimWithClaimer> toChannelClaimWithClaimer(CustomerDto customer) {
        return customer.getChannelClaims().stream()
                   .map(claim -> getChannelClaimWithClaimer(claim, customer));
    }

    @Override
    public Collection<ChannelClaimWithClaimer> getChannelClaimsForCustomer(URI cristinId) {
        return getChannelClaims().stream()
                   .filter(channelClaimWithClaimer -> channelClaimWithClaimer.isClaimedBy(cristinId))
                   .collect(Collectors.toSet());
    }

    private static ChannelClaimWithClaimer getChannelClaimWithClaimer(ChannelClaimDto channelClaim,
                                                                      CustomerDto customer) {
        return new ChannelClaimWithClaimer(channelClaim, customer.getId(), customer.getCristinId());
    }

    private static DynamoDbTable<CustomerDao> createTable(DynamoDbClient client) {
        var enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        return enhancedClient.table(CUSTOMERS_TABLE_NAME, CustomerDao.TABLE_SCHEMA);
    }

    private void warmupDynamoDbConnection(DynamoDbTable<CustomerDao> table) {
        try {
            table.describeTable();
        } catch (Exception e) {
            logger.warn(DYNAMODB_WARMUP_PROBLEM, e);
        }
    }

    private CustomerDto refreshCustomer(CustomerDto customer) {
        table.putItem(CustomerDao.fromCustomerDto(customer));
        return attempt(() -> getCustomer(customer.getIdentifier())).orElseThrow();
    }

    private CustomerDao createQueryForCristinNumber(URI cristinId) {
        return CustomerDao.builder().withCristinId(cristinId).build();
    }

    private void validateIdentifier(UUID identifier, CustomerDto customer) throws InputException {
        if (!identifier.equals(customer.getIdentifier())) {
            throw new InputException(String.format(IDENTIFIERS_NOT_EQUAL, identifier, customer.getIdentifier()), null);
        }
    }

    private CustomerDto addInternalDetails(CustomerDto customer) {
        Instant now = Instant.now();
        return customer.copy().withIdentifier(UUID.randomUUID()).withCreatedDate(now).withModifiedDate(now).build();
    }

    private void checkForConflict(URI institutionId) throws ConflictException {
        var customerExists = attempt(() -> getCustomerByCristinId(institutionId)).toOptional().isPresent();
        if (customerExists) {
            throw new ConflictException(String.format(CUSTOMER_ALREADY_EXISTS_ERROR, institutionId.toString()));
        }
    }

    private CustomerDto sendQueryToIndex(CustomerDao queryObject, String indexName,
                                         Function<CustomerDao, String> indexPartitionValue) throws NotFoundException {
        QueryEnhancedRequest query = createQuery(queryObject, indexPartitionValue);
        var results = table.index(indexName).query(query);
        return results.stream()
                   .flatMap(page -> page.items().stream())
                   .map(CustomerDao::toCustomerDto)
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> notFoundException(queryObject.toString()));
    }

    private QueryEnhancedRequest createQuery(CustomerDao queryObject,
                                             Function<CustomerDao, String> indexPartitionValue) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
            Key.builder().partitionValue(indexPartitionValue.apply(queryObject)).build());
        return QueryEnhancedRequest.builder().queryConditional(queryConditional).build();
    }

    private CustomerDao createQueryForOrgDomain(String feideDomain) {
        return CustomerDao.builder().withFeideOrganizationDomain(feideDomain).build();
    }

    private NotFoundException notFoundException(String queryValue) {
        return new NotFoundException(CUSTOMER_NOT_FOUND + queryValue);
    }
}
