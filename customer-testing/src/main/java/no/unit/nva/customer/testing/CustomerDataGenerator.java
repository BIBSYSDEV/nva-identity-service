package no.unit.nva.customer.testing;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDao;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDao;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintDto;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDao.RightsRetentionStrategyDao;
import no.unit.nva.customer.model.CustomerDao.ServiceCenterDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.PublicationWorkflow;
import no.unit.nva.customer.model.RightsRetentionStrategyType;
import no.unit.nva.customer.model.Sector;
import no.unit.nva.customer.model.VocabularyDao;
import no.unit.nva.customer.model.VocabularyDto;
import no.unit.nva.customer.model.VocabularyStatus;
import no.unit.nva.customer.model.interfaces.DoiAgent;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public final class CustomerDataGenerator {

    private static final String API_DOMAIN = new Environment().readEnv("API_DOMAIN");
    private static final String CRISTIN_PATH = "/cristin/organization";
    private static final String SERIAL_PUBLICATION = "serial-publication";
    private static final String PUBLISHER = "publisher";
    private static final List<String> CHANNEL_TYPES = List.of(SERIAL_PUBLICATION, PUBLISHER);

    private CustomerDataGenerator() {
        // Hidden constructor to prevent instantiation
    }

    public static CustomerDto createSampleCustomerDto() {
        UUID identifier = UUID.randomUUID();
        CustomerDto customer = CustomerDto.builder()
                                   .withName(randomString())
                                   .withCristinId(randomUri())
                                   .withCustomerOf(randomApplicationDomain())
                                   .withFeideOrganizationDomain(randomString())
                                   .withModifiedDate(randomInstant())
                                   .withIdentifier(identifier)
                                   .withCname(randomString())
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
                                   .withInactiveFrom(randomInstant())
                                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                                   .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                                   .withChannelClaims(randomChannelClaimDtos())
                                   .build();

        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));
        return customer;
    }

    public static List<ChannelClaimDto> randomChannelClaimDtos() {
        return List.of(randomChannelClaimDto(), randomChannelClaimDto(), randomChannelClaimDto());
    }

    public static ChannelClaimDto randomChannelClaimDto() {
        return new ChannelClaimDto(randomChannel(), randomChannelConstraintDto());
    }

    public static URI randomChannelOfType(String channelType) {
        if (!CHANNEL_TYPES.contains(channelType)) {
            throw new IllegalArgumentException();
        }
        var publicationChannelPath = new Environment().readEnv("PUBLICATION_CHANNEL_PATH");
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild(publicationChannelPath, channelType, UUID.randomUUID().toString())
                   .getUri();
    }

    public static URI randomChannel() {
        return randomChannelOfType(randomChannelType());
    }

    private static String randomChannelType() {
        return CHANNEL_TYPES.get(new Random().nextInt(CHANNEL_TYPES.size()));
    }

    public static List<ChannelClaimDao> randomChannelClaimDaos() {
        return List.of(randomChannelClaimDao(), randomChannelClaimDao(), randomChannelClaimDao());
    }

    public static ChannelClaimDao randomChannelClaimDao() {
        return new ChannelClaimDao(randomChannel(), randomChannelConstraintDao());
    }

    public static ChannelConstraintDao randomChannelConstraintDao() {
        return defaultChannelConstraintDao();

        // Commented out until restriction of constraints are removed
        // return new ChannelConstraintDao(randomChannelPolicy(), randomChannelPolicy(), randomScopes());
    }

    private static ChannelConstraintDao defaultChannelConstraintDao() {
        return new ChannelConstraintDao(ChannelConstraintPolicy.EVERYONE, ChannelConstraintPolicy.OWNER_ONLY, degreeScope());
    }

    public static ChannelConstraintDto randomChannelConstraintDto() {
        return defaultChannelConstraintDto();

        // Commented out until restriction of constraints are removed
        // return new ChannelConstraintDto(randomChannelPolicy(), randomChannelPolicy(), randomScopes());
    }

    private static ChannelConstraintDto defaultChannelConstraintDto() {
        return new ChannelConstraintDto(ChannelConstraintPolicy.EVERYONE, ChannelConstraintPolicy.OWNER_ONLY, degreeScope());
    }

    // Commented out until restriction of constraints are removed
    // public static ChannelConstraintPolicy randomChannelPolicy() {
    //    return randomElement(ChannelConstraintPolicy.values());
    // }

    // Commented out until restriction of constraints are removed
    // public static List<PublicationInstanceTypes> randomScopes() {
    //     return List.of(
    //         randomElement(PublicationInstanceTypes.values()),
    //         randomElement(PublicationInstanceTypes.values()),
    //         randomElement(PublicationInstanceTypes.values()));
    // }

    public static List<PublicationInstanceTypes> degreeScope() {
        return List.of(
            PublicationInstanceTypes.DEGREE_PHD,
            PublicationInstanceTypes.DEGREE_MASTER,
            PublicationInstanceTypes.DEGREE_BACHELOR,
            PublicationInstanceTypes.DEGREE_LICENTIATE,
            PublicationInstanceTypes.ARTISTIC_DEGREE_PHD,
            PublicationInstanceTypes.OTHER_STUDENT_WORK);
    }

    public static Set<VocabularyDto> randomVocabularyDtoSettings() {
        VocabularyDao vocabulary = randomVocabularyDao();
        return Stream.of(vocabulary)
                   .map(VocabularyDao::toVocabularySettingsDto)
                   .collect(Collectors.toSet());
    }

    public static VocabularyDao randomVocabularyDao() {
        return new VocabularyDao(randomString(), randomUri(),
                                 randomElement(VocabularyStatus.values()));
    }

    public static RightsRetentionStrategy randomRightsRetentionStrategy() {
        var elements = Arrays.stream(RightsRetentionStrategyType.values())
                           .filter(f -> f.ordinal() > 0)
                           .toList();
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

    public static PublicationWorkflow randomPublicationWorkflow() {
        return randomElement(PublicationWorkflow.values());
    }

    public static Sector randomSector() {
        return randomElement(Sector.values());
    }

    public static Set<PublicationInstanceTypes> randomAllowFileUploadForTypes() {
        var randomTypes = IntStream
                              .range(0, 3)
                              .mapToObj(i -> randomAllowFileUploadForTypesDto())
                              .collect(Collectors.toSet());
        return EnumSet.copyOf(randomTypes);
    }

    public static PublicationInstanceTypes randomAllowFileUploadForTypesDto() {
        return randomElement(PublicationInstanceTypes.values());
    }

    public static CustomerDao createSampleActiveCustomerDao() {
        var customer = createSampleInactiveCustomerDao();
        customer.setInactiveFrom(null);
        return customer;
    }

    public static CustomerDao createSampleInactiveCustomerDao() {
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
                                   .withServiceCenter(new ServiceCenterDao(randomUri(), randomString()))
                                   .withPublicationWorkflow(randomPublicationWorkflow())
                                   .withDoiAgent(randomDoiAgent(randomString()))
                                   .withNviInstitution(randomBoolean())
                                   .withSector(randomSector())
                                   .withInactiveFrom(randomInstant())
                                   .withRightsRetentionStrategy(randomRightsRetentionStrategy())
                                   .withAllowFileUploadForTypes(randomAllowFileUploadForTypes())
                                   .withGeneralSupportEnabled(true)
                                   .withChannelClaims(randomChannelClaimDaos())
                                   .build();
        assertThat(customer, doesNotHaveEmptyValuesIgnoringFields(Set.of("doiAgent.password")));
        return customer;
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

    public static List<VocabularyDto> randomVocabularies() {
        return List.of(randomVocabularyDto(), randomVocabularyDto(), randomVocabularyDto());
    }

    public static VocabularyDto randomVocabularyDto() {
        return new VocabularyDto(randomString(), randomUri(),
                                 randomElement(VocabularyStatus.values()));
    }

    private static ApplicationDomain randomApplicationDomain() {
        return ApplicationDomain.NVA;
    }
}



