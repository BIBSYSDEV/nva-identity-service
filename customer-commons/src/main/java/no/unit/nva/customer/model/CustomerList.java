package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import nva.commons.utils.JacocoGenerated;

@SuppressWarnings("PMD.ShortMethodName")
public class CustomerList {

    private List<CustomerDto> customers;
    @JsonProperty("@context")
    private URI context;

    @JacocoGenerated
    public CustomerList() {

    }

    public CustomerList(List<CustomerDto> customers) {
        this.customers = customers;
        this.context = CustomerMapper.context;
    }

    /**
     * Create a CustomerList from a List of CustomerDto objects.
     *
     * @param customers list of CustomerDto
     * @return customerList
     */
    public static CustomerList of(List<CustomerDto> customers) {
        return new CustomerList(customers);
    }

    public static CustomerList of(CustomerDto... customers) {
        return of(List.of(customers));
    }

    public List<CustomerDto> getCustomers() {
        return customers;
    }

    public void setCustomers(List<CustomerDto> customers) {
        this.customers = customers;
    }

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }
}
