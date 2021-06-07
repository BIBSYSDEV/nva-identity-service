package no.unit.nva.customer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomerListTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Test
    public void customerListFromCustomer() {
        CustomerDto customer = new CustomerDto();
        CustomerList customerList = CustomerList.of(customer);
        assertEquals(1, customerList.getCustomers().size());
        assertEquals(customer, customerList.getCustomers().get(0));
    }

    @Test
    public void customerListFromNull() {
        List<CustomerDto> list = new ArrayList<>();
        list.add(null);
        CustomerList customerList = CustomerList.of(list);
        assertEquals(1, customerList.getCustomers().size());
        assertTrue(customerList.getCustomers().get(0) == null);
    }

    @Test
    public void test() throws JsonProcessingException {
        CustomerDto customerDto = new CustomerDto();
        CustomerList customerList = new CustomerList(
            singletonList(customerDto));
        String customerListJson = objectMapper.writeValueAsString(customerList);
        CustomerList mappedCustomerList = objectMapper.readValue(customerListJson, CustomerList.class);
        assertNotNull(mappedCustomerList);
    }
}
