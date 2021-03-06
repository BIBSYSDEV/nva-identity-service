package no.unit.nva.customer.testing;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.PublicationWorkflow;
import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import nva.commons.core.paths.UriWrapper;

public class CustomerDataGenerator {

    private static final String API_HOST = "api.dev.aws.nva.unit.no";
    private static final String CRISTIN_PATH = "/cristin/organization";

    public static CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
                                   .withName(randomString())
                                   .withCristinId(randomUri())
                                   .withFeideOrganizationDomain(randomString())
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
                                   .withRorId(randomUri())
                                   .withPublicationWorkflow(randomPublicationWorkflow())
                                   .build();

        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    public static Set<VocabularyDto> randomVocabularyDtoSettings() {
        VocabularyDao vocabulary = randomVocabularyDao();
        return Stream.of(vocabulary)
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
                                   .withRorId(randomUri())
                                   .withPublicationWorkflow(randomPublicationWorkflow())
                                   .build();
        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
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

    public static PublicationWorkflow randomPublicationWorkflow() {
        return randomElement(PublicationWorkflow.values());
    }
}



