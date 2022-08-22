package no.unit.nva.customer;

import java.util.List;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MigrateCustomerHandlerTest extends LocalCustomerServiceDatabase {

    private DynamoDBCustomerService service;

    private MigrateCustomerHandler migrateCustomerHandler;

    @BeforeEach
    public void setUp() {
        super.setupDatabase();
        service = new DynamoDBCustomerService(dynamoClient);
    }

    @Test
    void attachCustomerOfDomainAttributeToNvaCustomers() {
        var expectedCustomers = service.getCustomers();
        attachCustomerOfDomainAttributeToCustomers(expectedCustomers);
        var actualCustomers = migrateCustomerHandler.attachCustomerOfDomainAttributeToCustomers(service.getCustomers());

    }

    private void attachCustomerOfDomainAttributeToCustomers(List<CustomerDto> customers) {
        customers.stream().forEach(customerDto -> customerDto.setCustomerOf(ApplicationDomain.NVA));
    }
}
