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
import nva.commons.core.paths.UriWrapper;

public class CustomerDataGenerator {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();
    private static final String API_HOST = "api.dev.aws.nva.unit.no";
    private static final String CRISTIN_PATH = "/cristin/organization";

    public static CustomerDao createSampleCustomerDao() {
        CustomerDao customer = new CustomerDao();

        customer.setName(randomString());
        customer.setCristinId(randomCristinOrgId().toString());
        customer.setFeideOrganizationId(randomString());
        customer.setModifiedDate(randomInstant());
        customer.setIdentifier(randomIdentifier());
        customer.setCname(randomString());
        customer.setArchiveName(randomString());
        customer.setShortName(randomString());
        customer.setInstitutionDns(randomString());
        customer.setDisplayName(randomString());
        customer.setCreatedDate(randomInstant());
        customer.setVocabularies(randomVocabularyDaoSettings());

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }


    public static CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);

        CustomerDto customer = new CustomerDto();

        customer.setId(id);
        customer.setName(randomString());
        customer.setCristinId(randomCristinOrgId().toString());
        customer.setFeideOrganizationId(randomString());
        customer.setModifiedDate(randomInstant());
        customer.setIdentifier(randomIdentifier());
        customer.setCname(randomString());
        customer.setArchiveName(randomString());
        customer.setShortName(randomString());
        customer.setInstitutionDns(randomString());
        customer.setDisplayName(randomString());
        customer.setCreatedDate(randomInstant());
        customer.setVocabularies(randomVocabularyDtoSettings());

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    public static Set<VocabularyDao> randomVocabularyDaoSettings() {
        VocabularyDao vocabulary = randomVocabulary();
        return Set.of(vocabulary)
            .stream()
            .collect(Collectors.toSet());
    }

    public static Set<VocabularyDto> randomVocabularyDtoSettings() {
        VocabularyDao vocabulary = randomVocabulary();
        return Set.of(vocabulary)
                .stream()
                .map(VocabularyDao::toVocabularySettingsDto)
                .collect(Collectors.toSet());
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

    private static VocabularyDao randomVocabulary() {
        return new VocabularyDao(randomString(), randomUri(),
                                 randomElement(VocabularyStatus.values()));
    }
}



