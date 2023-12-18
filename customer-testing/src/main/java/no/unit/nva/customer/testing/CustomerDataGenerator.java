package no.unit.nva.customer.testing;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.LinkedDataContextUtils;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.PublicationWorkflow;
import no.unit.nva.customer.model.CustomerDao.RightsRetentionStrategyDao;
import no.unit.nva.customer.model.RightsRetentionStrategyType;
import no.unit.nva.customer.model.Sector;
import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class CustomerDataGenerator {
    
    private static final String API_DOMAIN = new Environment().readEnv("API_DOMAIN");
    private static final String CRISTIN_PATH = "/cristin/organization";
    
    public static CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        URI id = LinkedDataContextUtils.toId(identifier);
        CustomerDto customer = CustomerDto.builder()
                                   .withName(randomString())
                                   .withCristinId(randomUri())
                                   .withCustomerOf(randomApplicationDomain())
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
                                   .withDoiAgent(randomDoiAgent(randomString()))
                                   .withSector(randomSector())
                                   .withNviInstitution(randomBoolean())
                                   .withRboInstitution(randomBoolean())
                                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                                   .withAllowFileUploadFor(randomAllowFileUploadFor())
                                   .build();

        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));
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
                                   .withCustomerOf(randomApplicationDomainUri())
                                   .withVocabularySettings(Set.of(vocabulary))
                                   .withInstitutionDns(randomString())
                                   .withFeideOrganizationDomain(randomString())
                                   .withDisplayName(randomString())
                                   .withCreatedDate(randomInstant())
                                   .withCname(randomString())
                                   .withArchiveName(randomString())
                                   .withRorId(randomUri())
                                   .withPublicationWorkflow(randomPublicationWorkflow())
                                   .withDoiAgent(randomDoiAgent(randomString()))
                                   .withNviInstitution(randomBoolean())
                                   .withSector(randomSector())
                                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                                   .withAllowFileUploadFor(randomAllowFileUploadFor())
                                   .build();
        assertThat(customer, doesNotHaveEmptyValues());
        return customer;
    }

    public static RightsRetentionStrategy randomRightsRetentionStrategy() {
        var elements = Arrays.stream(RightsRetentionStrategyType.values())
                .filter(f -> f.ordinal() > 0)
                .collect(Collectors.toList());
        return
            new RightsRetentionStrategyDao(randomElement(elements), randomUri());
    }

    public static DoiAgent randomDoiAgent(String randomString) {
        return new DoiAgent() {

            @Override
            public String getPrefix() {
                return "10.000";
            }

            @Override
            public String getUrl() {
                return "mds." + randomString + ".datacite.org";
            }

            @Override
            public String getUsername() {
                return "user-name-" + randomString;
            }
        };
    }

    private static ApplicationDomain randomApplicationDomain() {
        return ApplicationDomain.NVA;
    }

    public static URI randomApplicationDomainUri() {
        return URI.create(ApplicationDomain.NVA.toString());
    }

    public static UUID randomIdentifier() {
        return UUID.randomUUID();
    }

    public static URI randomCristinOrgId() {
        return new UriWrapper("https", API_DOMAIN)
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

    public static Sector randomSector() {
        return randomElement(Sector.values());
    }

    public static PublicationInstanceTypes randomAllowFileUploadForDto() {
        return randomElement(PublicationInstanceTypes.values());
    }

    public static Set<PublicationInstanceTypes> randomAllowFileUploadFor() {
        return new HashSet<>(Arrays.asList(randomAllowFileUploadForDto(), randomAllowFileUploadForDto(),
                                           randomAllowFileUploadForDto()));
    }
}



