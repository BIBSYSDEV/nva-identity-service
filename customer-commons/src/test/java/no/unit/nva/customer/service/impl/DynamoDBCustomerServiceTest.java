package no.unit.nva.customer.service.impl;

import static no.unit.nva.customer.model.VocabularyStatus.ALLOWED;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.CUSTOMERS_TABLE_NAME;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomAllowFileUploadForTypes;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelClaimDtos;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomChannelConstraintDto;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomCristinOrgId;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRightsRetentionStrategy;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomSector;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
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
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

class DynamoDBCustomerServiceTest extends LocalCustomerServiceDatabase {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCustomerServiceTest.class);
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
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        assertNotNull(createdCustomer.getIdentifier());
        //inject automatically generated id
        customer.setId(createdCustomer.getId());
        assertThat(createdCustomer, is(equalTo(createdCustomer)));
    }

    @Test
    void updateExistingCustomerWithNewName() throws NotFoundException, InputException, ConflictException {
        String newName = "New name";
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertNotEquals(newName, createdCustomer.getName());

        createdCustomer.setName(newName);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(newName, updatedCustomer.getName());
    }

    @Test
    void shouldRefreshCustomers() throws ConflictException, NotFoundException {
        service.createCustomer(newActiveCustomerDto());
        assertDoesNotThrow(() -> service.refreshCustomers());
    }

    @Test
    void shouldUpdateRboInstitutionWhenRboInstitutionIsSetToTrue()
        throws NotFoundException, InputException, ConflictException {
        var customer = newActiveCustomerDto();
        customer.setRboInstitution(false);
        var createdCustomer = service.createCustomer(customer);
        assertFalse(createdCustomer.isRboInstitution());

        createdCustomer.setRboInstitution(true);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertTrue(updatedCustomer.isRboInstitution());
    }

    @Test
    void shouldUpdateInactiveFromWhenInactiveIsSet() throws NotFoundException, InputException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertThat(createdCustomer.getInactiveFrom(), is(nullValue()));

        var now = Instant.now();
        createdCustomer.setInactiveFrom(now);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertThat(updatedCustomer.getInactiveFrom(), is(equalTo(now)));
    }

    @Test
    void updateExistingCustomerChangesModifiedDate() throws NotFoundException, InputException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertNotEquals(customer.getModifiedDate(), updatedCustomer.getModifiedDate());
    }

    @Test
    void updateExistingCustomerPreservesCreatedDate() throws NotFoundException, InputException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(createdCustomer.getCreatedDate(), updatedCustomer.getCreatedDate());
    }

    @Test
    void updateExistingCustomerWithDifferentIdentifiersThrowsException() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var differentIdentifier = UUID.randomUUID();
        var exception = assertThrows(InputException.class,
                                     () -> service.updateCustomer(differentIdentifier, createdCustomer));
        var expectedMessage = String.format(DynamoDBCustomerService.IDENTIFIERS_NOT_EQUAL, differentIdentifier,
                                            createdCustomer.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void getExistingCustomerReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = null;
        try {
            getCustomer = service.getCustomer(createdCustomer.getIdentifier());
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
        }
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void shouldReturnCustomerById() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto retrievedCustomer = null;
        try {
            retrievedCustomer = service.getCustomer(createdCustomer.getId());
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
        }
        assertThat(createdCustomer, is(equalTo(retrievedCustomer)));
    }

    @Test
    void getCustomerByOrgDomainReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = null;
        try {
            getCustomer = service.getCustomerByOrgDomain(createdCustomer.getFeideOrganizationDomain());
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
        }
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void getCustomerByCristinIdReturnsTheCustomer() throws NotFoundException, ConflictException {
        var customer = newInactiveCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertThat(createdCustomer, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));
        var retrievedCustomer = service.getCustomerByCristinId(createdCustomer.getCristinId());
        assertEquals(createdCustomer, retrievedCustomer);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenQueryResultIsEmpty() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        service.createCustomer(customer);
        assertThrows(NotFoundException.class, () -> service.getCustomerByCristinId(randomUri()));
    }

    @Test
    void getAllCustomersReturnsListOfCustomers() throws NotFoundException, ConflictException {
        // create three customers
        service.createCustomer(newActiveCustomerDto());
        service.createCustomer(newActiveCustomerDto());
        service.createCustomer(newActiveCustomerDto());

        var customers = service.getCustomers();
        assertEquals(3, customers.size());
    }

    @Test
    void getCustomerNotFoundThrowsException() {
        var nonExistingCustomer = UUID.randomUUID();
        var exception = assertThrows(NotFoundException.class, () -> service.getCustomer(nonExistingCustomer));
        assertThat(exception.getMessage(), Matchers.containsString(nonExistingCustomer.toString()));
    }

    @Test
    void getCustomerTableErrorThrowsException() {
        final var expectedMessage = randomString();
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        when(failingTable.getItem(any(CustomerDao.class))).thenAnswer(ignored -> {
            throw new RuntimeException(expectedMessage);
        });
        var failingService = new DynamoDBCustomerService(failingTable);
        var exception = assertThrows(RuntimeException.class, () -> failingService.getCustomer(UUID.randomUUID()));
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
        }).when(failingTable).putItem(any(CustomerDao.class));
        var failingService = new DynamoDBCustomerService(failingTable);
        var exception = assertThrows(RuntimeException.class,
                                     () -> failingService.createCustomer(newInactiveCustomerDto()));
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void updateCustomerTableErrorThrowsException() {
        DynamoDbTable<CustomerDao> failingTable = mock(DynamoDbTable.class);
        final var expectedMessage = randomString();
        doAnswer(ignored -> {
            throw new RuntimeException(expectedMessage);
        }).when(failingTable).putItem(any(CustomerDao.class));
        var failingService = new DynamoDBCustomerService(failingTable);
        var customer = newActiveCustomerDto();
        customer.setIdentifier(UUID.randomUUID());
        var exception = assertThrows(RuntimeException.class,
                                     () -> failingService.putCustomer(customer.getIdentifier(), customer, false));
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
        assertThat(updatedCustomer.getVocabularies().getFirst().getStatus(), is(equalTo(ALLOWED)));
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
    void shouldCreateChannelClaimForCustomer() throws ConflictException, NotFoundException,
                                                      InputException, BadRequestException {
        var customer = createCustomerWithoutChannelClaim();
        var channelClaim = randomChannelClaimDto();
        service.createChannelClaim(customer.getIdentifier(), channelClaim);
        var updatedCustomer = service.getCustomer(customer.getIdentifier());

        assertTrue(updatedCustomer.getChannelClaims().contains(channelClaim));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenCreatingChannelClaimWithInvalidChannel()
        throws NotFoundException, ConflictException {
        var customer = createCustomerWithoutChannelClaim();
        var channelClaim = new ChannelClaimDto(randomUri(), randomChannelConstraintDto());

        assertThrows(BadRequestException.class,
                     () -> service.createChannelClaim(customer.getIdentifier(), channelClaim));
    }

    @Test
    void shouldThrowConflictExceptionWhenTheCustomerAlreadyHaveClaimedTheChannel() throws ConflictException,
                                                                                   NotFoundException {
        var existingClaim = randomChannelClaimDto();
        var customer = createCustomerWithChannelClaim(existingClaim);

        assertThrows(ConflictException.class, () -> service.createChannelClaim(customer.getIdentifier(), existingClaim));
    }

    @Test
    void shouldThrowConflictExceptionWhenAnotherCustomerHasAlreadyClaimedTheChannel() throws ConflictException,
                                                                                   NotFoundException {
        var existingClaim = randomChannelClaimDto();
        createCustomerWithChannelClaim(existingClaim);

        var customer = createCustomerWithoutChannelClaim();
        assertThrows(ConflictException.class, () -> service.createChannelClaim(customer.getIdentifier(), existingClaim));
    }

    @Test
    void shouldIgnoreChannelClaimsWhenUpdatingCustomer() throws ConflictException, NotFoundException,
                                                                   InputException {
        var customer = createCustomerWithoutChannelClaim();
        customer.overwriteChannelClaims(randomChannelClaimDtos());

        var updatedCustomer = service.updateCustomer(customer.getIdentifier(), customer);
        assertTrue(updatedCustomer.getChannelClaims().isEmpty());
    }

    @Test
    void shouldReturnAllChannelClaims() throws ConflictException, NotFoundException {
        createCustomerWithChannelClaim(randomChannelClaimDto());
        createCustomerWithChannelClaim(randomChannelClaimDto());

        var allChannelClaims = service.getChannelClaims();
        assertEquals(2, allChannelClaims.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoCustomersHasAnyChannelClaims() throws ConflictException, NotFoundException {
        createCustomerWithoutChannelClaim();

        var allChannelClaims = service.getChannelClaims();
        assertEquals(0, allChannelClaims.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoCustomersExists() {
        var allChannelClaims = service.getChannelClaims();
        assertEquals(0, allChannelClaims.size());
    }

    @Test
    void shouldReturnCustomerIdentifierAndCristinIdWhenRequestingChannelClaims()
        throws ConflictException, NotFoundException {
        var channelClaim = randomChannelClaimDto();
        var customer = createCustomerWithChannelClaim(channelClaim);

        var allChannelClaims = service.getChannelClaims();
        var actualChannelClaim = allChannelClaims.stream().findFirst().orElseThrow();
        assertEquals(customer.getId(), actualChannelClaim.customerId());
        assertEquals(customer.getCristinId(), actualChannelClaim.cristinId());
        assertEquals(channelClaim, actualChannelClaim.channelClaim());
    }

    @Test
    void shouldListChannelClaimsForInstitution()
        throws ConflictException, NotFoundException {
        var customer = createCustomerWithChannelClaim(randomChannelClaimDto());
        createCustomerWithChannelClaim(randomChannelClaimDto());
        createCustomerWithChannelClaim(randomChannelClaimDto());

        var channelClaims = service.getChannelClaimsForCustomer(customer.getCristinId());

        assertEquals(1, channelClaims.size());
        assertEquals(customer.getCristinId(), channelClaims.stream().findFirst().orElseThrow().cristinId());
    }

    @Test
    void shouldReturnChannelClaimByChannelIdentifier() throws ConflictException, NotFoundException {
        var channelClaim = randomChannelClaimDto();
        createCustomerWithChannelClaim(channelClaim);

        var channelClaimIdentifier = channelClaim.identifier();
        var fetchedClaim = service.getChannelClaim(channelClaimIdentifier);

        assertEquals(channelClaim, fetchedClaim.orElseThrow().channelClaim());
    }

    @Test
    void shouldRemoveChannelClaimFromCustomer() throws ConflictException, NotFoundException, InputException {
        var channelClaim = randomChannelClaimDto();
        createCustomerWithChannelClaim(channelClaim);

        service.deleteChannelClaim(channelClaim.identifier());
        var fetchedChannelClaim = service.getChannelClaim(channelClaim.identifier());

        assertTrue(fetchedChannelClaim.isEmpty());
    }

    @Test
    void shouldNotCallUpdateCustomerWhenRemovingNotExistingChannelClaim() throws NotFoundException, InputException {
        var mockedService = mock(DynamoDBCustomerService.class);
        mockedService.deleteChannelClaim(UUID.randomUUID());

        verify(mockedService, never())
            .putCustomer(any(), any(), eq(false));
    }

    private CustomerDto newActiveCustomerDto() {
        var customer = newInactiveCustomerDto();
        customer.setInactiveFrom(null);
        return customer;
    }

    private CustomerDto newInactiveCustomerDto() {
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
                           .withInactiveFrom(randomInstant())
                           .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                           .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                           .withChannelClaims(randomChannelClaimDtos())
                           .build();
        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(
            Set.of("identifier", "id", "context", "doiAgent.password", "doiAgent.id")));
        return customer;
    }

    private Set<VocabularyDto> randomVocabularySet() {
        return Set.of(randomVocabulary(), randomVocabulary());
    }

    private VocabularyDto randomVocabulary() {
        return new VocabularyDto(randomString(), randomUri(), randomElement(VocabularyStatus.values()));
    }

    private String extractVocabularyStatusFromCustomerEntryContainingExactlyOneVocabulary(
        Map<String, AttributeValue> updatedEntry) {
        return updatedEntry.get(CustomerDao.VOCABULARIES_FIELD)
                   .l()
                   .getFirst()
                   .m()
                   .get(VocabularyDao.STATUS_FIELD)
                   .s();
    }

    private void updateDatabaseEntryWithVocabularyStatusHavingAlternateCase(Map<String, AttributeValue> entry) {
        var newEntry = createNewEntryWithVocabularyStatusHavingAlternateCase(entry);
        this.dynamoClient.putItem(PutItemRequest.builder().item(newEntry).tableName(CUSTOMERS_TABLE_NAME).build());
    }

    private HashMap<String, AttributeValue> createNewEntryWithVocabularyStatusHavingAlternateCase(
        Map<String, AttributeValue> entry) {
        var vocabulary = new HashMap<>(entry.get(CustomerDao.VOCABULARIES_FIELD).l().getFirst().m());
        vocabulary.put(VocabularyDao.STATUS_FIELD, AttributeValue.builder().s(statusWithAlternateCase()).build());
        var newEntry = new HashMap<>(entry);
        AttributeValue vocabularyEntry = AttributeValue.builder().m(vocabulary).build();
        var newVocabulariesList = AttributeValue.builder().l(vocabularyEntry).build();
        newEntry.put(CustomerDao.VOCABULARIES_FIELD, newVocabulariesList);
        return newEntry;
    }

    private String statusWithAlternateCase() {
        return "AlLoWed";
    }

    private Map<String, AttributeValue> fetchCustomerDirectlyFromDatabaseAsKeyValueMap() {
        var allEntries = this.dynamoClient.scan(ScanRequest.builder().tableName(CUSTOMERS_TABLE_NAME).build());
        return allEntries.items().getFirst();
    }

    private CustomerDto createCustomerWithSingleVocabularyEntry() throws NotFoundException, ConflictException {
        var customer = newInactiveCustomerDto();
        customer.setVocabularies(List.of(randomVocabulary()));
        return service.createCustomer(customer);
    }

    private CustomerDto createCustomerWithoutChannelClaim() throws NotFoundException, ConflictException {
        var customer = newActiveCustomerDto();
        return service.createCustomer(customer.overwriteChannelClaims(Collections.emptyList()));
    }

    private CustomerDto createCustomerWithChannelClaim(ChannelClaimDto channelClaim) throws NotFoundException,
                                                                                   ConflictException {
        var customer = newActiveCustomerDto();
        return service.createCustomer(customer.overwriteChannelClaims(List.of(channelClaim)));
    }
}
