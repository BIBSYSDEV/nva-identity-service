package no.unit.nva.customer.service.impl;

import static no.unit.nva.customer.DynamoConfig.defaultDynamoConfigMapper;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.ERROR_MAPPING_CUSTOMER_TO_ITEM;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.ERROR_MAPPING_ITEM_TO_CUSTOMER;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.ERROR_READING_FROM_TABLE;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.ERROR_WRITING_ITEM_TO_TABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import no.unit.nva.customer.exception.DynamoDBException;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.exception.NotFoundException;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import nva.commons.core.Environment;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DynamoDBCustomerServiceTest extends CustomerDynamoDBLocal {

    private DynamoDBCustomerService service;
    private Environment environment;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        environment = new Environment();
        service = new DynamoDBCustomerService(
            defaultDynamoConfigMapper,
            getTable(),
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
    }

    @Test
    public void testConstructorThrowsNoExceptions() {
        CustomerService serviceWithTableNameFromEnv = new DynamoDBCustomerService(ddb, defaultDynamoConfigMapper, environment);
        assertNotNull(serviceWithTableNameFromEnv);
    }

    @Test
    public void createNewCustomerReturnsTheCustomer() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);

        assertNotNull(createdCustomer.getIdentifier());
        //inject automatically generated id
        customer.setId(createdCustomer.getId());
        assertThat(createdCustomer, is(equalTo(createdCustomer)));
    }

    @Test
    public void updateExistingCustomerWithNewName() throws Exception {
        String newName = "New name";
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);
        assertNotEquals(newName, createdCustomer.getName());

        createdCustomer.setName(newName);
        CustomerDto updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(newName, updatedCustomer.getName());
    }

    @Test
    public void updateExistingCustomerChangesModifiedDate() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);

        CustomerDto updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertNotEquals(customer.getModifiedDate(), updatedCustomer.getModifiedDate());
    }

    @Test
    public void updateExistingCustomerPreservesCreatedDate() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);

        CustomerDto updatedCustomer = service.updateCustomer(createdCustomer.getIdentifier(), createdCustomer);
        assertEquals(customer.getCreatedDate(), updatedCustomer.getCreatedDate());
    }

    @Test
    public void updateExistingCustomerWithDifferentIdentifiersThrowsException() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);
        UUID differentIdentifier = UUID.randomUUID();

        InputException exception = assertThrows(InputException.class,
                                                () -> service.updateCustomer(differentIdentifier, createdCustomer));
        String expectedMessage = String.format(DynamoDBCustomerService.IDENTIFIERS_NOT_EQUAL,
                                               differentIdentifier, customer.getIdentifier());
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void getExistingCustomerReturnsTheCustomer() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = service.getCustomer(createdCustomer.getIdentifier());
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    public void getCustomerByOrgNumberReturnsTheCustomer() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = service.getCustomerByOrgNumber(createdCustomer.getFeideOrganizationId());
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    public void getCustomerByCristinIdReturnsTheCustomer() throws Exception {
        CustomerDto customer = getNewCustomer();
        CustomerDto createdCustomer = service.createCustomer(customer);
        CustomerDto getCustomer = service.getCustomerByCristinId(createdCustomer.getCristinId());
        assertEquals(createdCustomer, getCustomer);
    }

    @Test
    public void getAllCustomersReturnsListOfCustomers() throws Exception {
        // create three customers
        service.createCustomer(getNewCustomer());
        service.createCustomer(getNewCustomer());
        service.createCustomer(getNewCustomer());

        List<CustomerDto> customers = service.getCustomers();
        assertEquals(3, customers.size());
    }

    @Test
    public void getCustomerNotFoundThrowsException() {
        UUID nonExistingCustomer = UUID.randomUUID();
        NotFoundException exception = assertThrows(NotFoundException.class,
                                                   () -> service.getCustomer(nonExistingCustomer));
        assertThat(exception.getMessage(), Matchers.containsString(nonExistingCustomer.toString()));
    }

    @Test
    public void getCustomerTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.getItem(anyString(), any())).thenThrow(RuntimeException.class);
        DynamoDBCustomerService failingService = new DynamoDBCustomerService(
            defaultDynamoConfigMapper,
            failingTable,
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class,
                                                   () -> failingService.getCustomer(UUID.randomUUID()));
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    @Test
    public void getCustomersTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.scan()).thenThrow(RuntimeException.class);
        DynamoDBCustomerService failingService = new DynamoDBCustomerService(
            defaultDynamoConfigMapper,
            failingTable,
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class, () -> failingService.getCustomers());
        assertEquals(ERROR_READING_FROM_TABLE, exception.getMessage());
    }

    @Test
    public void createCustomerTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBCustomerService failingService = new DynamoDBCustomerService(
            defaultDynamoConfigMapper,
            failingTable,
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
        DynamoDBException exception = assertThrows(DynamoDBException.class,
                                                   () -> failingService.createCustomer(getNewCustomer()));
        assertEquals(ERROR_WRITING_ITEM_TO_TABLE, exception.getMessage());
    }

    @Test
    public void updateCustomerTableErrorThrowsException() {
        Table failingTable = mock(Table.class);
        when(failingTable.putItem(any(Item.class))).thenThrow(RuntimeException.class);
        DynamoDBCustomerService failingService = new DynamoDBCustomerService(
            defaultDynamoConfigMapper,
            failingTable,
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
        CustomerDto customer = getNewCustomer();
        customer.setIdentifier(UUID.randomUUID());
        DynamoDBException exception = assertThrows(DynamoDBException.class,
                                                   () -> failingService.updateCustomer(customer.getIdentifier(),
                                                                                       customer));
        assertEquals(ERROR_WRITING_ITEM_TO_TABLE, exception.getMessage());
    }

    @Test
    public void customerToItemThrowsExceptionWhenInvalidJson() throws JsonProcessingException {
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(any(CustomerDao.class))).thenThrow(JsonProcessingException.class);
        DynamoDBCustomerService failingService = new DynamoDBCustomerService(
            failingObjectMapper,
            getTable(),
            getByOrgNumberIndex(),
            getByCristinIdIndex()
        );
        InputException exception = assertThrows(InputException.class,
                                                () -> failingService.customerToItem(getNewCustomer()));
        assertEquals(ERROR_MAPPING_CUSTOMER_TO_ITEM, exception.getMessage());
    }

    @Test
    public void itemToCustomerThrowsExceptionWhenInvalidJson() {
        Item item = mock(Item.class);
        when(item.toJSON()).thenThrow(new IllegalStateException());
        DynamoDBException exception = assertThrows(DynamoDBException.class,
                                                   () -> service.itemToCustomer(item));
        assertEquals(ERROR_MAPPING_ITEM_TO_CUSTOMER, exception.getMessage());
    }

    private CustomerDto getNewCustomer() {
        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);
        return CustomerDto.builder()
            .withName("Name")
            .withShortName("SN")
            .withCreatedDate(oneMinuteInThePast)
            .withModifiedDate(oneMinuteInThePast)
            .withDisplayName("Display Name")
            .withArchiveName("Archive Name")
            .withCname("CNAME")
            .withInstitutionDns("institution.dns")
            .withFeideOrganizationId("123456789")
            .withCristinId("http://cristin.id")
            .build();
    }

    private Index getByOrgNumberIndex() {
        return getIndex(CustomerDynamoDBLocal.BY_ORG_NUMBER_INDEX_NAME);
    }

    private Index getByCristinIdIndex() {
        return getIndex(CustomerDynamoDBLocal.BY_CRISTIN_ID_INDEX_NAME);
    }
}
