package no.unit.nva.customer.model;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CustomerListTest {

    @Test
    void customerListFromCustomer() {
        CustomerDto customer = new CustomerDto();
        CustomerList customerList = new CustomerList(List.of(customer));
        assertEquals(1, customerList.getCustomers().size());
        assertEquals(CustomerReference.fromCustomerDto(customer), customerList.getCustomers().get(0));
    }

    @Test
    void customerListFromNull() {
        List<CustomerDto> list = new ArrayList<>();
        list.add(null);
        CustomerList customerList = new CustomerList(list);
        assertEquals(0, customerList.getCustomers().size());
    }

    @Test
    void customerListCanBeConvertedToJsonAndBack() {
        List<CustomerDto> customerDtos = List.of(new CustomerDto());
        CustomerList customerList = new CustomerList(customerDtos);
        String customerListJson = customerList.toString();
        CustomerList mappedCustomerList = CustomerList.fromString(customerListJson);
        assertNotNull(mappedCustomerList);
    }

    @Test
    void jacocoTestForTestCoverage() {
        var customerList = new CustomerList().getCustomers();
        assertEquals(Collections.emptyList(), customerList);
    }

    @ParameterizedTest
    @EnumSource(value = Sector.class)
    void shouldIncludeSectorInCustomerReferenceWhenConvertingCustomersToCustomerList(Sector sector) {
        var customer = customerWithSector(sector);
        var customerList = new CustomerList(List.of(customer));

        var sectorFromCustomerReference = customerList.getCustomers().getFirst().getSector();

        assertEquals(customer.getSector(), sectorFromCustomerReference);
    }

    private static CustomerDto customerWithSector(Sector sector) {
        return CustomerDto.builder()
                   .withIdentifier(randomUUID())
                   .withSector(sector)
                   .build();
    }
}
