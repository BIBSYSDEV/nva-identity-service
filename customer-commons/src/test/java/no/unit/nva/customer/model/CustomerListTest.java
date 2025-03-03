package no.unit.nva.customer.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        var cust = new CustomerList().getCustomers();
        assertEquals(Collections.emptyList(), cust);
    }
}
