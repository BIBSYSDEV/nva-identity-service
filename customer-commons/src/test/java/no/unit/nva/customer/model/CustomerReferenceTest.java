package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDto;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerReferenceTest {


    @Test
    void shouldCreateCustomerReferenceFromCustomerDto() {
        var customerDto = CustomerDto.builder()
                              .withId(randomUri())
                              .withDisplayName(randomString())
                              .withCreatedDate(Instant.now())
                              .build();
        var expectedCustomerReference = constructExpectedCustomerReference(customerDto);

        assertEquals(expectedCustomerReference, CustomerReference.fromCustomerDto(customerDto));
    }

    @Test
    void shouldIncludeNviInformationInCustomerReference() {
        var customerDto = createSampleCustomerDto().copy().withNviInstitution(true).build();
        var customerReference = CustomerReference.fromCustomerDto(customerDto);

        assertEquals(customerReference.getCristinId(), customerDto.getCristinId());
        assertEquals(customerReference.isNviInstitution(), customerDto.isNviInstitution());
    }

    private static CustomerReference constructExpectedCustomerReference(CustomerDto customerDto) {
        var customerReference = new CustomerReference();
        customerReference.setActive(true);
        customerReference.setCreatedDate(customerDto.getCreatedDate());
        customerReference.setDoiPrefix(null);
        customerReference.setId(customerDto.getId());
        customerReference.setDisplayName(customerDto.getDisplayName());
        return customerReference;
    }
}