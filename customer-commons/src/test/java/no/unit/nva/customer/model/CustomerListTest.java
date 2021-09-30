package no.unit.nva.customer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomerListTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Test
    public void customerListFromCustomer() {
        CustomerDtoWithoutContext customer = new CustomerDtoWithoutContext();
        CustomerList customerList = CustomerMapper.toCustomerListFromCustomerDtosWithoutContexts(List.of(customer));
        assertEquals(1, customerList.getCustomers().size());
        assertEquals(customer, customerList.getCustomers().get(0));
    }

    @Test
    public void customerListFromNull() {
        List<CustomerDtoWithoutContext> list = new ArrayList<>();
        list.add(null);
        CustomerList customerList = CustomerMapper.toCustomerListFromCustomerDtosWithoutContexts(list);
        assertEquals(1, customerList.getCustomers().size());
        assertTrue(customerList.getCustomers().get(0) == null);
    }

    @Test
    public void customerListCanBeConvertedToJsonAndBack() throws JsonProcessingException {
        List<CustomerDtoWithoutContext> customerDtos = List.of(new CustomerDtoWithoutContext());
        CustomerList customerList = CustomerMapper.toCustomerListFromCustomerDtosWithoutContexts(customerDtos);
        String customerListJson = objectMapper.writeValueAsString(customerList);
        CustomerList mappedCustomerList = objectMapper.readValue(customerListJson, CustomerList.class);
        assertNotNull(mappedCustomerList);
    }
}
