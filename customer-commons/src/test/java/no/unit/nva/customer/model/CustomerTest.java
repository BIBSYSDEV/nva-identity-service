package no.unit.nva.customer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.customer.ObjectMapperConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.customer.model.VocabularyStatus.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomerTest {

    private final ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;

    @Test
    public void customerMappedToJsonAndBack() throws JsonProcessingException {
        CustomerDb customer = createCustomerDb();

        CustomerDb mappedCustomer = objectMapper.readValue(objectMapper.writeValueAsString(customer), CustomerDb.class);

        assertEquals(customer, mappedCustomer);
        assertThat(customer, doesNotHaveNullOrEmptyFields());
    }

    @Test
    public void customerMapperCanMapBetweenCustomerDtoAndCustomerDb() {
        CustomerDb customerDb = createCustomerDb();
        CustomerDto customerDto = customerDb.toCustomerDto();

        assertNotNull(customerDto);
        assertNotNull(customerDto.getId());

        CustomerDb mappedCustomerDB = CustomerDb.fromCustomerDto(customerDto);
        assertNotNull(mappedCustomerDB);
    }

    @Test
    public void customerMapperCanMapCustomerDbToCustomerDto() {
        CustomerDb customerDb = createCustomerDb();
        CustomerDto customerDto = customerDb.toCustomerDto();
        assertNotNull(customerDto);
        assertNotNull(customerDto.getContext());
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
    public void vocacularySettingsDoesNotContainDuplicates() {
        CustomerDb customerDb = createCustomerDb();
        customerDb.getVocabularySettings().add(vocabularySetting());

        assertThat(customerDb.getVocabularySettings().size(), Matchers.is(Matchers.equalTo(1)));
    }

    private CustomerDb createCustomerDb() {
        Instant now = Instant.now();

        Set<VocabularySettingDb> vocabularySettings = new HashSet<>();
        vocabularySettings.add(vocabularySetting());

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
            .withVocabularySettings(vocabularySettings)
            .build();
    }

    private VocabularySettingDb vocabularySetting() {
        return new VocabularySettingDb(
            "Vocabulary A",
            URI.create("http://uri.to.vocabulary.a"),
            VocabularyStatus.lookup("Default")
        );
    }
}
