package no.unit.nva.customer.service.impl;

import static no.unit.nva.customer.model.VocabularyStatus.ALLOWED;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.CUSTOMERS_TABLE_NAME;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomCristinOrgId;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRetentionStrategy;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomSector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class DynamoDBCustomerServiceTest extends LocalCustomerServiceDatabase {

    public static final int SINGLE_VOCABULARY = 0;
    private DynamoDBCustomerService service;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        service = new DynamoDBCustomerService(dynamoClient);
    }

    @Test
    void createNewCustomerReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        assertNotNull(createdCustomer.getIdentifier());
        //inject automatically generated id
        customer.setId(createdCustomer.getId());
        assertThat(createdCustomer, is(equalTo(createdCustomer)));
    }

    @Test
    void updateExistingCustomerWithNewName() throws NotFoundException, InputException, ConflictException {
        String newName = "New name";
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertNotEquals(newName, createdCustomer.getName());

        createdCustomer.setName(newName);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(newName, updatedCustomer.getName());
    }

    @Test
    void shouldUpdateRboInstitutionWhenRboInstitutionIsSetToTrue() throws NotFoundException, InputException, ConflictException {
        var customer = newCustomerDto();
        customer.setRboInstitution(false);
        var createdCustomer = service.createCustomer(customer);
        assertFalse(createdCustomer.isRboInstitution());

        createdCustomer.setRboInstitution(true);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertTrue(updatedCustomer.isRboInstitution());
    }

    @Test
    void updateExistingCustomerChangesModifiedDate() throws NotFoundException, InputException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertNotEquals(customer.getModifiedDate(), updatedCustomer.getModifiedDate());
    }

    @Test
    void updateExistingCustomerPreservesCreatedDate() throws NotFoundException, InputException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(createdCustomer.getCreatedDate(), updatedCustomer.getCreatedDate());
    }

    @Test
    void updateExistingCustomerWithDifferentIdentifiersThrowsException() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var differentIdentifier = UUID.randomUUID();
        var exception = assertThrows(InputException.class,
                                     () -> service.updateCustomer(differentIdentifier, createdCustomer));
        var expectedMessage = String.format(DynamoDBCustomerService.IDENTIFIERS_NOT_EQUAL,
                                            differentIdentifier, createdCustomer.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void getExistingCustomerReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = null;
        try {
            getCustomer = service.getCustomer(createdCustomer.getIdentifier());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void shouldReturnCustomerById() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto retrievedCustomer = null;
        try {
            retrievedCustomer = service.getCustomer(createdCustomer.getId());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        assertThat(createdCustomer, is(equalTo(retrievedCustomer)));
    }

    @Test
    void getCustomerByOrgDomainReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = null;
        try {
            getCustomer = service.getCustomerByOrgDomain(createdCustomer.getFeideOrganizationDomain());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void getCustomerByCristinIdReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertThat(createdCustomer, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));
        var retrievedCustomer = service.getCustomerByCristinId(createdCustomer.getCristinId());
        assertEquals(createdCustomer, retrievedCustomer);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenQueryResultIsEmpty() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        service.createCustomer(customer);
        assertThrows(NotFoundException.class, () -> service.getCustomerByCristinId(randomUri()));
    }

    @Test
    void getAllCustomersReturnsListOfCustomers() throws NotFoundException, ConflictException {
        // create three customers
        service.createCustomer(newCustomerDto());
        service.createCustomer(newCustomerDto());
        service.createCustomer(newCustomerDto());

        var customers = service.getCustomers();
        assertEquals(3, customers.size());
    }

    @Test
    void getCustomerNotFoundThrowsException() {
        var nonExistingCustomer = UUID.randomUUID();
        var exception = assertThrows(NotFoundException.class,
                                     () -> service.getCustomer(nonExistingCustomer));
        assertThat(exception.getMessage(), Matchers.containsString(nonExistingCustomer.toString()));
    }

    @Test
    void getCustomerTableErrorThrowsException() {
        final var expectedMessage = randomString();
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        when(failingTable.getItem(any(CustomerDao.class)))
            .thenAnswer(ignored -> {
                throw new RuntimeException(expectedMessage);
            });
        var failingService = new DynamoDBCustomerService(failingTable);
        var exception = assertThrows(RuntimeException.class,
                                     () -> failingService.getCustomer(UUID.randomUUID()));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void getCustomersTableErrorThrowsException() {
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        final var expectedMessage = randomString();
        when(failingTable.scan()).thenAnswer(ignored -> {
            throw new RuntimeException(expectedMessage);
        });
        var failingService = new DynamoDBCustomerService(failingTable);
        var exception = assertThrows(RuntimeException.class, failingService::getCustomers);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void createCustomerTableErrorThrowsException() {
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        final var expectedMessage = randomString();
        doAnswer(ignored -> {
            throw new RuntimeException(expectedMessage);
        })
            .when(failingTable).putItem(any(CustomerDao.class));
        var failingService = new DynamoDBCustomerService(failingTable);
        var exception = assertThrows(RuntimeException.class,
                                     () -> failingService.createCustomer(newCustomerDto()));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void updateCustomerTableErrorThrowsException() {
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        final var expectedMessage = randomString();
        doAnswer(ignored -> {
            throw new RuntimeException(expectedMessage);
        })
            .when(failingTable).putItem(any(CustomerDao.class));
        var failingService = new DynamoDBCustomerService(failingTable);
        var customer = newCustomerDto();
        customer.setIdentifier(UUID.randomUUID());
        var exception = assertThrows(RuntimeException.class,
                                     () -> failingService.updateCustomer(customer.getIdentifier(),
                                                                         customer));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldReadEntryWhereVocabularyStatusIsNotCamelCase() throws NotFoundException, ConflictException {
        var savedCustomer = createCustomerWithSingleVocabularyEntry();
        var entry = fetchCustomerDirectlyFromDatabaseAsKeyValueMap();
        updateDatabaseEntryWithVocabularyStatusHavingAlternateCase(entry);
        var updatedEntry = fetchCustomerDirectlyFromDatabaseAsKeyValueMap();
        var updatedVocabularyStatus = extractVocabularyStatusFromCustomerEntryContainingExactlyOneVocabulary(
            updatedEntry);
        assertThat(updatedVocabularyStatus, is(equalTo(statusWithAlternateCase())));
        var updatedCustomer = service.getCustomer(savedCustomer.getIdentifier());
        assertThat(updatedCustomer.getVocabularies().get(0).getStatus(), is(equalTo(ALLOWED)));
    }

    @Test
    void shouldThrowConflictErrorWhenCustomerWithSameInstitutionIdExists() throws NotFoundException, ConflictException {
        var existingCustomer = createCustomerWithSingleVocabularyEntry();
        var customerDuplicate = CustomerDto.builder()
            .withCristinId(existingCustomer.getCristinId())
            .withCname(randomString())
            .withArchiveName(randomString())
            .withName(randomString())
            .build();
        Executable action = () -> service.createCustomer(customerDuplicate);
        assertThrows(ConflictException.class, action);
    }

    @Test
    void shouldUpdateCustomerOfAttributeToExistingNvaCustomers() throws ConflictException, NotFoundException {
        var existingCustomer = createCustomerWithSingleVocabularyEntry();
        var expectedCustomer = addCustomerOfNvaAttribute(existingCustomer);
        var actualCustomer = service.updateCustomersWithNvaAttribute().get(0);

        assertThat(expectedCustomer.getCustomerOf(), is(equalTo(actualCustomer.getCustomerOf())));
    }

    private CustomerDto addCustomerOfNvaAttribute(CustomerDto customerDto) {
        customerDto.setCustomerOf(randomElement(List.of(ApplicationDomain.values())));
        return customerDto;
    }

    private String extractVocabularyStatusFromCustomerEntryContainingExactlyOneVocabulary(
        Map<String, AttributeValue> updatedEntry) {
        return updatedEntry.get(CustomerDao.VOCABULARIES_FIELD).l().get(SINGLE_VOCABULARY)
            .m().get(VocabularyDao.STATUS_FIELD).s();
    }

    private void updateDatabaseEntryWithVocabularyStatusHavingAlternateCase(Map<String, AttributeValue> entry) {
        var newEntry = createNewEntryWithVocabularyStatusHavingAlternateCase(entry);
        this.dynamoClient.putItem(PutItemRequest.builder().item(newEntry).tableName(CUSTOMERS_TABLE_NAME).build());
    }

    private HashMap<String, AttributeValue> createNewEntryWithVocabularyStatusHavingAlternateCase(
        Map<String, AttributeValue> entry) {
        var vocabulary = new HashMap<>(entry.get(CustomerDao.VOCABULARIES_FIELD).l().get(SINGLE_VOCABULARY).m());
        vocabulary.put(VocabularyDao.STATUS_FIELD, AttributeValue.builder().s(statusWithAlternateCase()).build());
        var newEntry = new HashMap<>(entry);
        AttributeValue vocabularyEntry = AttributeValue.builder().m(vocabulary).build();
        var newVocabulariesList = AttributeValue.builder().l(vocabularyEntry).build();
        newEntry.put(CustomerDao.VOCABULARIES_FIELD, newVocabulariesList);
        return newEntry;
    }

    private Map<String, AttributeValue> fetchCustomerDirectlyFromDatabaseAsKeyValueMap() {
        var allEntries = this.dynamoClient.scan(ScanRequest.builder().tableName(CUSTOMERS_TABLE_NAME).build());
        return allEntries.items().get(0);
    }

    private CustomerDto createCustomerWithSingleVocabularyEntry() throws NotFoundException, ConflictException {
        var customer = newCustomerDto();
        customer.setVocabularies(List.of(randomVocabulary()));
        return service.createCustomer(customer);
    }

    private String statusWithAlternateCase() {
        return "AlLoWed";
    }


    private CustomerDto newCustomerDto() {
        var oneMinuteInThePast = Instant.now().minusSeconds(60L);
        var customer = CustomerDto.builder()
                           .withName(randomString())
                           .withShortName(randomString())
                           .withCreatedDate(oneMinuteInThePast)
                           .withModifiedDate(oneMinuteInThePast)
                           .withDisplayName(randomString())
                           .withArchiveName(randomString())
                           .withCname(randomString())
                           .withInstitutionDns(randomString())
                           .withFeideOrganizationDomain(randomString())
                           .withCristinId(randomCristinOrgId())
                           .withCustomerOf(ApplicationDomain.fromUri(URI.create("")))
                           .withVocabularies(randomVocabularySet())
                           .withRorId(randomUri())
                           .withPublicationWorkflow(randomPublicationWorkflow())
                           .withDoiAgent(randomDoiAgent(randomString()))
                           .withSector(randomSector())
                           .withNviInstitution(randomBoolean())
                           .withRboInstitution(randomBoolean())
                           .withRightRetentionStrategy(randomRetentionStrategy())
                           .build();
        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("identifier", "id", "context",
                                                                         "doiAgent.password","doiAgent.id")));
        return customer;
    }

    private Set<VocabularyDto> randomVocabularySet() {
        return Set.of(randomVocabulary(), randomVocabulary());
    }

    private VocabularyDto randomVocabulary() {
        return new VocabularyDto(randomString(), randomUri(), randomElement(VocabularyStatus.values()));
    }
}
