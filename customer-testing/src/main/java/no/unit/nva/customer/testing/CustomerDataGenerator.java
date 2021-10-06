package no.unit.nva.customer.testing;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;

public class CustomerDataGenerator {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();

    public static CustomerDto createSampleCustomerDto() {
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
            .withContext(LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE)
            .withArchiveName(randomString())
            .withShortName(randomString())
            .withInstitutionDns(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant())
            .withVocabularies(randomVocabularyDtoSettings())
            .build();

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    public static Set<VocabularyDto> randomVocabularyDtoSettings() {
        return randomVocabularySettings()
            .stream()
            .map(VocabularyDao::toVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    public static CustomerDao createSampleCustomerDb() {
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

    public static Set<VocabularyDao> randomVocabularySettings() {
        VocabularyDao vocabulary = new VocabularyDao(randomString(), randomUri(),
                                                     randomElement(VocabularyStatus.values()));
        return Set.of(vocabulary);
    }

    public static <T> T randomElement(T... values) {
        return values[RANDOM.nextInt(values.length)];
    }

    public static URI randomUri() {
        return URI.create("https://www.example.com/" + FAKER.lorem().word() + FAKER.lorem().word());
    }

    public static Instant randomInstant() {
        return FAKER.date().between(Date.from(Instant.now().minus(Period.ofDays(10))),
                                    Date.from(Instant.now())).toInstant();
    }

    public static String randomString() {
        return FAKER.lorem().sentence(2);
    }

    public static UUID randomIdentifier() {
        return UUID.randomUUID();
    }
}



