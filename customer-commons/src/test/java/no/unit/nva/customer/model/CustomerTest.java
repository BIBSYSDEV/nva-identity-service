package no.unit.nva.customer.model;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ObjectMapperConfig;
import org.junit.jupiter.api.Test;

public class CustomerTest {

    private final ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;
    private CustomerMapper customerMapper = new CustomerMapper("http://example.org/customer");

    @Test
    public void customerMappedToJsonAndBack() throws JsonProcessingException {
        CustomerDb customer = createCustomerDb();

        CustomerDb mappedCustomer = objectMapper.readValue(objectMapper.writeValueAsString(customer), CustomerDb.class);

        assertEquals(customer, mappedCustomer);
        assertThat(customer, doesNotHaveNullOrEmptyFields());
    }

    @Test
    public void customerMapperCanMapBetweenCustomerDtoAndCustomerDb() throws JsonProcessingException {
        CustomerDb customerDb = createCustomerDb();
        CustomerDto customerDto = customerMapper.toCustomerDto(customerDb);

        System.out.println(objectMapper.writeValueAsString(customerDto));

        assertNotNull(customerDto);
        assertNotNull(customerDto.getId());

        CustomerDb mappedCustomerDB = customerMapper.toCustomerDb(customerDto);
        assertNotNull(mappedCustomerDB);
    }

    @Test
    public void customerMapperCanMapListOfCustomerDtosToCustomerList() {
        CustomerDb customerDb = createCustomerDb();
        CustomerList customerList = customerMapper.toCustomerList(Collections.singletonList(customerDb));
        assertNotNull(customerList);
    }

    @Test
    public void customerMapperCanMapustomerDbToCustomerDtoWithoutContext() {
        CustomerDb customerDb = createCustomerDb();
        CustomerDto customerDto = customerMapper.toCustomerDtoWithoutContext(customerDb);
        assertNotNull(customerDto);
        assertNull(customerDto.getContext());
    }

    @Test
    public void lookupUnknownVocabularyStatusThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> VocabularyStatus.lookup("Unknown"));
        assertNotNull(exception);
    }

    private CustomerDb createCustomerDb() {
        Instant now = Instant.now();
        return new CustomerDb.Builder()
            .withIdentifier(UUID.randomUUID())
            .withName("Name")
            .withShortName("SN")
            .withCreatedDate(now)
            .withModifiedDate(now)
            .withDisplayName("Display Name")
            .withArchiveName("Archive Name")
            .withCname("CNAME")
            .withInstitutionDns("institution.dns")
            .withFeideOrganizationId("123456789")
            .withCristinId("http://cristin.id")
            .withVocabularySettings(Set.of(vocabularySetting()))
            .build();
    }

    private VocabularySetting vocabularySetting() {
        return VocabularySetting.Builder.builder()
                .withName("Vocabulary A")
                .withId(URI.create("http://uri.to.vocabulary.a"))
                .withStatus(VocabularyStatus.lookup("Default"))
                .build();
    }
}
