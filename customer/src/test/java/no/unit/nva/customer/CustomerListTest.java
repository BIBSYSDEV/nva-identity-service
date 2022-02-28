package no.unit.nva.customer;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.responses.CustomerListResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomerListTest {

    @Test
    public void customerListFromCustomer() {
        CustomerDto customer = new CustomerDto();
        CustomerListResponse customerList = new CustomerListResponse(List.of(customer));
        assertEquals(1, customerList.getCustomers().size());
        assertEquals(customer, customerList.getCustomers().get(0));
    }

    @Test
    public void customerListCanBeConvertedToJsonAndBack() throws JsonProcessingException {
        List<CustomerDto> customerDtos = List.of(new CustomerDto());
        CustomerListResponse customerList = new CustomerListResponse(customerDtos);
        String customerListJson = defaultRestObjectMapper.writeValueAsString(customerList);
        CustomerListResponse mappedCustomerList = defaultRestObjectMapper.readValue(
                customerListJson, CustomerListResponse.class);
        assertNotNull(mappedCustomerList);
    }
}
