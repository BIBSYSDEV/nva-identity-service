package no.unit.nva.customer.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.customer.model.VocabularyStatus.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomApplicationDomainUri;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomCristinOrgId;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomPublicationWorkflow;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomRetentionStrategy;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.testing.CustomerDataGenerator;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class CustomerTest {

    @Test
    void customerMapperCanMapBetweenCustomerDtoAndCustomerDb() {
        CustomerDao customerDb = createCustomerDb();
        CustomerDto customerDto = customerDb.toCustomerDto();

        assertNotNull(customerDto);
        assertNotNull(customerDto.getId());

        CustomerDao mappedCustomerDB = CustomerDao.fromCustomerDto(customerDto);
        assertNotNull(mappedCustomerDB);
    }

    @Test
    void customerMapperCanMapCustomerDbToCustomerDto() {
        CustomerDao customerDb = createCustomerDb();
        CustomerDto customerDto = customerDb.toCustomerDto();
        assertNotNull(customerDto);
        assertNotNull(customerDto.getContext());
    }

    @Test
    void lookupUnknownVocabularyStatusThrowsIllegalArgumentException() {
        String value = "Unknown";
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                                                       () -> VocabularyStatus.lookUp(value));

        String expectedMessage = format(ERROR_MESSAGE_TEMPLATE, value,
                                        stream(VocabularyStatus.values())
                                            .map(VocabularyStatus::toString)
                                            .collect(joining(VocabularyStatus.DELIMITER)));

        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void vocacularySettingsDoesNotContainDuplicates() {
        CustomerDao customerDb = createCustomerDb();
        customerDb.getVocabularies().add(vocabularySetting());

        assertThat(customerDb.getVocabularies().size(), Matchers.is(Matchers.equalTo(1)));
    }

    private CustomerDao createCustomerDb() {
        Instant now = Instant.now();

        Set<VocabularyDao> vocabularySettings = new HashSet<>();
        vocabularySettings.add(vocabularySetting());

        return new CustomerDao.Builder()
            .withIdentifier(UUID.randomUUID())
            .withName("Name")
            .withShortName("SN")
                   .withCreatedDate(now)
                   .withModifiedDate(now)
                   .withDisplayName("Display Name")
                   .withArchiveName("Archive Name")
                   .withCname("CNAME")
                   .withInstitutionDns("institution.dns")
                   .withFeideOrganizationDomain("123456789")
                   .withCristinId(randomCristinOrgId())
                   .withCustomerOf(randomApplicationDomainUri())
                   .withVocabularySettings(vocabularySettings)
                   .withRorId(randomUri())
                   .withPublicationWorkflow(randomPublicationWorkflow())
                   .withDoiAgent(CustomerDataGenerator.randomDoiAgent(randomString()))
                   .withRightRetentionStrategy(randomRetentionStrategy())
            .build();
    }

    private VocabularyDao vocabularySetting() {
        return new VocabularyDao(
            "Vocabulary A",
            URI.create("http://uri.to.vocabulary.a"),
            VocabularyStatus.lookUp("Default")
        );
    }
}
