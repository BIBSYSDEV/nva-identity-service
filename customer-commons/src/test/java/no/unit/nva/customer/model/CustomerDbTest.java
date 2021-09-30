package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

class CustomerDbTest {

    public static final Faker FAKER = Faker.instance();
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Javers JAVERS = JaversBuilder.javers().build();

    @Test
    public void toCustomerDtoReturnsDtoWithoutLossOfInformation() {
        CustomerDao expected = createSampleCustomerDb();
        CustomerDto customerDto = expected.toCustomerDto();
        CustomerDao actual = CustomerDao.fromCustomerDto(customerDto);
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDto, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void fromCustomerDbReturnsDbWithoutLossOfInformation() {
        CustomerDto expected = crateSampleCustomerDto();
        CustomerDao customerDb = CustomerDao.fromCustomerDto(expected);
        CustomerDto actual = customerDb.toCustomerDto();
        Diff diff = JAVERS.compare(expected, actual);
        assertThat(customerDb, doesNotHaveEmptyValues());
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(actual, is(equalTo(expected)));
    }

    private CustomerDto crateSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
            .withName(randomString())
            .withCristinId(randomString())
            .withFeideOrganizationId(randomString())
            .withModifiedDate(randomInstant())
            .withIdentifier(identifier)
            .withId(id)
            .withCname(randomString())
            .withContext(LINKED_DATA_CONTEXT_VALUE)
            .withArchiveName(randomString())
            .withShortName(randomString())
            .withInstitutionDns(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant())
            .withVocabularySettings(randomVocabularyDtoSettings())
            .build();

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    private Set<VocabularySettingDto> randomVocabularyDtoSettings() {
        return randomVocabularySettings()
            .stream()
            .map(VocabularySettingDb::toVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    private CustomerDao createSampleCustomerDb() {
        CustomerDao customer = CustomerDao.builder()
            .withIdentifier(randomIdentifier())
            .withName(randomString())
            .withModifiedDate(randomInstant())
            .withShortName(randomString())
            .withCristinId(randomString())
            .withVocabularySettings(randomVocabularySettings())
            .withInstitutionDns(randomString())
            .withFeideOrganizationId(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant())
            .withCname(randomString())
            .withArchiveName(randomString())
            .build();
        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    private Set<VocabularySettingDb> randomVocabularySettings() {
        VocabularySettingDb vocabulary = new VocabularySettingDb(randomString(), randomUri(),
                                                                 randomElement(VocabularyStatus.values()));
        return Set.of(vocabulary);
    }

    private <T> T randomElement(T[] values) {
        return values[RANDOM.nextInt(values.length)];
    }

    private URI randomUri() {
        return URI.create("https://www.example.com/" + FAKER.lorem().word() + FAKER.lorem().word());
    }

    private Instant randomInstant() {
        return FAKER.date().between(Date.from(Instant.now().minus(Period.ofDays(10))),
                                    Date.from(Instant.now())).toInstant();
    }

    private String randomString() {
        return FAKER.lorem().sentence(2);
    }

    private UUID randomIdentifier() {
        return UUID.randomUUID();
    }
}