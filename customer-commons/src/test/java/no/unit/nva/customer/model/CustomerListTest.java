package no.unit.nva.customer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomerListTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private final CustomerMapper customerMapper = new CustomerMapper("http://example.org/customer");

    @Test
    public void customerListFromCustomer() {
        CustomerDto customer = new CustomerDto();
        CustomerList customerList = customerMapper.toCustomerListFromCustomerDtos(List.of(customer));
        assertEquals(1, customerList.getCustomers().size());
        assertEquals(customer, customerList.getCustomers().get(0));
    }

    @Test
    public void customerListFromNull() {
        List<CustomerDto> list = new ArrayList<>();
        list.add(null);
        CustomerList customerList = customerMapper.toCustomerListFromCustomerDtos(list);
        assertEquals(1, customerList.getCustomers().size());
        assertTrue(customerList.getCustomers().get(0) == null);
    }

    @Test
    public void test() throws JsonProcessingException {
        List<CustomerDto> customerDtos = List.of(new CustomerDto());
        CustomerList customerList = customerMapper.toCustomerListFromCustomerDtos(customerDtos);
        String customerListJson = objectMapper.writeValueAsString(customerList);
        CustomerList mappedCustomerList = objectMapper.readValue(customerListJson, CustomerList.class);
        assertNotNull(mappedCustomerList);
    }
}
