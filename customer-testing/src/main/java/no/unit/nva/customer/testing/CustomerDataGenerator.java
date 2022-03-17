package no.unit.nva.customer.testing;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.List;
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
import nva.commons.core.paths.UriWrapper;

public class CustomerDataGenerator {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();
    private static final String API_HOST = "api.dev.aws.nva.unit.no";
    private static final String CRISTIN_PATH = "/cristin/organization";

    public static CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
            .withName(randomString())
            .withCristinId(randomUri())
            .withFeideOrganizationId(randomString())
            .withModifiedDate(randomInstant().toString())
            .withIdentifier(identifier)
            .withId(id)
            .withCname(randomString())
            .withContext(LinkedDataContextUtils.LINKED_DATA_CONTEXT_VALUE)
            .withArchiveName(randomString())
            .withShortName(randomString())
            .withInstitutionDns(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant().toString())
            .withVocabularies(randomVocabularyDtoSettings())
            .build();

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    public static Set<VocabularyDto> randomVocabularyDtoSettings() {
        VocabularyDao vocabulary = randomVocabularyDao();
        return Set.of(vocabulary)
            .stream()
            .map(VocabularyDao::toVocabularySettingsDto)
            .collect(Collectors.toSet());
    }

    public static CustomerDao createSampleCustomerDao() {
        VocabularyDao vocabulary = randomVocabularyDao();
        CustomerDao customer = CustomerDao.builder()
            .withIdentifier(randomIdentifier())
            .withName(randomString())
            .withModifiedDate(randomInstant())
            .withShortName(randomString())
            .withCristinId(randomUri())
            .withVocabularySettings(Set.of(vocabulary))
            .withInstitutionDns(randomString())
            .withFeideOrganizationDomain(randomString())
            .withDisplayName(randomString())
            .withCreatedDate(randomInstant())
            .withCname(randomString())
            .withArchiveName(randomString())
            .build();
        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
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

    public static URI randomCristinOrgId() {
        return new UriWrapper("https", API_HOST)
            .addChild(CRISTIN_PATH)
            .addChild(randomString())
            .getUri();
    }

    public static VocabularyDao randomVocabularyDao() {
        return new VocabularyDao(randomString(), randomUri(),
                                 randomElement(VocabularyStatus.values()));
    }

    public static VocabularyDto randomVocabularyDto() {
        return new VocabularyDto(randomString(), randomUri(),
                                 randomElement(VocabularyStatus.values()));
    }

   public static List<VocabularyDto> randomVocabularies() {
        return List.of(randomVocabularyDto(), randomVocabularyDto(), randomVocabularyDto());
    }
}



