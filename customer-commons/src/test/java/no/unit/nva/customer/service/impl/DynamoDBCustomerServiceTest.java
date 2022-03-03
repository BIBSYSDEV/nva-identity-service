package no.unit.nva.customer.service.impl;

import static no.unit.nva.customer.testing.CustomerDataGenerator.randomCristinOrgId;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomElement;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomString;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomUri;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

class DynamoDBCustomerServiceTest extends CustomerDynamoDBLocal {

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
    void createNewCustomerReturnsTheCustomer() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        assertNotNull(createdCustomer.getIdentifier());
        //inject automatically generated id
        customer.setId(createdCustomer.getId());
        assertThat(createdCustomer, is(equalTo(createdCustomer)));
    }

    @Test
    void updateExistingCustomerWithNewName() {
        String newName = "New name";
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertNotEquals(newName, createdCustomer.getName());

        createdCustomer.setName(newName);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(newName, updatedCustomer.getName());
    }

    @Test
    void updateExistingCustomerChangesModifiedDate() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);

        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertNotEquals(customer.getModifiedDate(), updatedCustomer.getModifiedDate());
    }

    @Test
    void updateExistingCustomerPreservesCreatedDate() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(customer.getCreatedDate(), updatedCustomer.getCreatedDate());
    }

    @Test
    void updateExistingCustomerWithDifferentIdentifiersThrowsException() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var differentIdentifier = UUID.randomUUID();
        var exception = assertThrows(InputException.class,
                                     () -> service.updateCustomer(differentIdentifier, createdCustomer));
        var expectedMessage = String.format(DynamoDBCustomerService.IDENTIFIERS_NOT_EQUAL,
                                            differentIdentifier, customer.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void getExistingCustomerReturnsTheCustomer() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var getCustomer = service.getCustomer(createdCustomer.getIdentifier());
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void getCustomerByOrgNumberReturnsTheCustomer() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        var getCustomer = service.getCustomerByOrgNumber(createdCustomer.getFeideOrganizationId());
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    void getCustomerByCristinIdReturnsTheCustomer() {
        var customer = newCustomerDto();
        var createdCustomer = service.createCustomer(customer);
        assertThat(createdCustomer, doesNotHaveEmptyValues());
        var retrievedCustomer = service.getCustomerByCristinId(createdCustomer.getCristinId());
        assertEquals(createdCustomer, retrievedCustomer);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenQueryResultIsEmpty() {
        var customer = newCustomerDto();
        service.createCustomer(customer);
        assertThrows(NotFoundException.class, () -> service.getCustomerByCristinId(randomUri().toString()));
    }

    @Test
    void getAllCustomersReturnsListOfCustomers() {
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

    private CustomerDto newCustomerDto() {
        var oneMinuteInThePast = Instant.now().minusSeconds(60L);
        var customer = CustomerDto.builder()
            .withName(randomString())
            .withShortName(randomString())
            .withCreatedDate(oneMinuteInThePast.toString())
            .withModifiedDate(oneMinuteInThePast.toString())
            .withDisplayName(randomString())
            .withArchiveName(randomString())
            .withCname(randomString())
            .withInstitutionDns(randomString())
            .withFeideOrganizationId(randomString())
            .withCristinId(randomCristinOrgId().toString())
            .withVocabularies(randomVocabularySet())
            .build();
        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("identifier", "id", "context")));
        return customer;
    }

    private Set<VocabularyDto> randomVocabularySet() {
        return Set.of(randomVocabulary(), randomVocabulary());
    }

    private VocabularyDto randomVocabulary() {
        return new VocabularyDto(randomString(), randomUri(), randomElement(VocabularyStatus.values()));
    }
}
