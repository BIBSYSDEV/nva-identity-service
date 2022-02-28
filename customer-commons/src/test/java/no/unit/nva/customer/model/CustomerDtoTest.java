package no.unit.nva.customer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.customer.JsonConfig.defaultDynamoConfigMapper;
import static no.unit.nva.customer.model.VocabularyStatus.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDto;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomerDtoTest {

    @Test
    public void customerMappedToJsonAndBack() throws JsonProcessingException {
        CustomerDto customer = CustomerDataGenerator.createSampleCustomerDto();
        CustomerDto mappedCustomer = defaultDynamoConfigMapper.readValue(
                defaultDynamoConfigMapper.writeValueAsString(customer), CustomerDto.class);

        assertEquals(customer, mappedCustomer);
        assertThat(customer, doesNotHaveEmptyValues());
    }

    @Test
    public void customerMapperCanMapCustomerDbToCustomerDto() {
        CustomerDao customerDb = createSampleCustomerDao();
        CustomerDto customerDto = customerDb.toCustomerDto();
        assertNotNull(customerDto);
        assertNotNull(customerDto.getId());
    }

    @Test
    public void lookupUnknownVocabularyStatusThrowsIllegalArgumentException() {
        String value = "Unknown";
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                                                       () -> VocabularyStatus.lookup(value));

        String expectedMessage = format(ERROR_MESSAGE_TEMPLATE, value,
                                        stream(VocabularyStatus.values())
                                            .map(VocabularyStatus::toString)
                                            .collect(joining(VocabularyStatus.DELIMITER)));

        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    public void vocabularySettingsDoesNotContainDuplicates() {
        CustomerDto customerDto = createSampleCustomerDto();

        assertThat(customerDto.getVocabularies().size(), Matchers.is(Matchers.equalTo(1)));
    }
}
