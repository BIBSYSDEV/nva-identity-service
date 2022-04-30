package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.function.Predicate.not;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.usercreation.cristin.person.PersonAndInstitutionRegistryClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.paths.UriWrapper;

public class CristinProxyMock {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final boolean INACTIVE = false;
    public static final int LARGE_NUMBER_TO_AVOID_TEST_SUCCESS_BY_LACK = 100;
    public static final int LARGE_NUMBER_TO_ENSURE_THE_EXISTENCE_OF_BOTH_ACTIVE_AND_INACTIVE_AFFILIATIONS = 50;
    private static final Boolean MATCH_CASE = false;
    private static final boolean ACTIVE = true;
    private final Set<NationalIdentityNumber> peopleMatchedToSomeScenario;
    private final NvaAuthServerMock dataporten;
    private final URI cristinPersonHost;
    private final Map<NationalIdentityNumber, CristinPersonResponse> cristinPersonRegistry;
    private Set<NationalIdentityNumber> people;
    private NationalIdentityNumber personWithOneActiveAffiliationAndNoInactiveAffiliations;
    private NationalIdentityNumber personWithNoActiveAffiliations;
    private NationalIdentityNumber personWithActiveAndInactiveAffiliations;
    private NationalIdentityNumber personWithManyActiveAffiliations;
    private NationalIdentityNumber personWithActiveAffiliationThatIsNotCustomer;
    private List<URI> parentInstitutionsThatAreNvaCustomers;
    private Set<URI> organizations;
    private Map<URI, URI> organizationToParentInstitutionMap;
    private URI parentInstitutionThatIsNotNvaCustomer;

    public CristinProxyMock(WireMockServer httpServer,
                            NvaAuthServerMock dataportenMock) {
        this.cristinPersonRegistry = new ConcurrentHashMap<>();
        peopleMatchedToSomeScenario = new HashSet<>();
        this.dataporten = dataportenMock;
        cristinPersonHost = URI.create(httpServer.baseUrl());
    }

    public void initialize(Set<NationalIdentityNumber> people) {
        this.people = people;
        createImaginaryOrganizationStructure();
        createPersonWithOneActiveAffiliation();
        createPersonWithNoActiveAffiliations();
        createPersonWithActiveAndInactiveAffiliations();
        createPersonWithActiveAffiliationThatIsNotCustomer();
        createPersonWithManyActiveAffiliations();
    }

    public NationalIdentityNumber getPersonWithOneActiveAffiliationAndNoInactiveAffiliations() {
        return personWithOneActiveAffiliationAndNoInactiveAffiliations;
    }

    public List<URI> getParentInstitutionsThatAreNvaCustomers() {
        return this.parentInstitutionsThatAreNvaCustomers;
    }

    public NationalIdentityNumber getPersonWithOnlyInactiveAffiliations() {
        return personWithNoActiveAffiliations;
    }

    public CristinPersonResponse getCristinPersonRecord(NationalIdentityNumber personLoggingIn) {
        return this.cristinPersonRegistry.get(personLoggingIn);
    }

    public URI randomOrgFromTheAffiliationsPool() {
        return randomElement(organizations);
    }

    public URI randomPersonUri() {
        return UriWrapper.fromUri(cristinPersonHost).addChild("person").addChild(randomString()).getUri();
    }

    public URI getParentInstitutionForOrganization(URI uri) {
        return organizationToParentInstitutionMap.get(uri);
    }

    public NationalIdentityNumber getPersonWithManyActiveAffiliations() {
        return personWithManyActiveAffiliations;
    }

    public NationalIdentityNumber getPersonWithActiveAndInactiveAffiliations() {
        return personWithActiveAndInactiveAffiliations;
    }

    public NationalIdentityNumber getPersonWithActiveAffiliationThatIsNotCustomer() {
        return personWithActiveAffiliationThatIsNotCustomer;
    }

    private URI getParentInstitutionThatIsNotNvaCustomer() {
        return parentInstitutionThatIsNotNvaCustomer;
    }

    private void createPersonWithActiveAffiliationThatIsNotCustomer() {
        personWithActiveAffiliationThatIsNotCustomer = nextPerson();
        var affiliationThatIsNotCustomer = CristinAffiliation.builder()
            .withActive(ACTIVE)
            .withOrganization(getParentInstitutionThatIsNotNvaCustomer())
            .build();
        createCristinRecord(personWithActiveAffiliationThatIsNotCustomer, List.of(affiliationThatIsNotCustomer));
        createStubResponseForPerson(personWithActiveAffiliationThatIsNotCustomer);
    }

    private URI createRandomOrgUriForTheImaginarySetup() {
        return UriWrapper.fromUri(cristinPersonHost).addChild("organization").addChild(randomString()).getUri();
    }

    private void createImaginaryOrganizationStructure() {
        createParentInstitutions();
        setupOrganizations();
        createParentInstitutionThatIsNotACustomer();
    }

    private void setupOrganizations() {
        var personAffiliations = createOrganizationsAttachedToParentInstitutions();
        organizationToParentInstitutionMap = new HashMap<>();
        personAffiliations.forEach(
            entry -> organizationToParentInstitutionMap.put(entry.getOrganization(), entry.getParentInstitution()));
    }

    private void createParentInstitutions() {
        parentInstitutionsThatAreNvaCustomers = smallSetOfParentInstitutions();
    }

    private Set<PersonAffiliation> createOrganizationsAttachedToParentInstitutions() {
        var organizations = setOfOrganizationsSignificantlyLargerThanParentInstitutions();
        organizations.forEach(this::attachCristinOrgResponseToStub);
        this.organizations =
            organizations.stream().map(PersonAffiliation::getOrganization).collect(Collectors.toSet());
        return organizations;
    }

    private void createParentInstitutionThatIsNotACustomer() {
        parentInstitutionThatIsNotNvaCustomer = createRandomOrgUriForTheImaginarySetup();
        organizationToParentInstitutionMap.put(parentInstitutionThatIsNotNvaCustomer,
                                               parentInstitutionThatIsNotNvaCustomer);
        attachCristinOrgResponseToStub(
            PersonAffiliation.create(parentInstitutionThatIsNotNvaCustomer, parentInstitutionThatIsNotNvaCustomer));
    }

    private Set<PersonAffiliation> setOfOrganizationsSignificantlyLargerThanParentInstitutions() {
        return intStream(LARGE_NUMBER_TO_ENSURE_THE_EXISTENCE_OF_BOTH_ACTIVE_AND_INACTIVE_AFFILIATIONS)
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .map(bottomLevelOrgUri ->
                     PersonAffiliation.create(bottomLevelOrgUri, randomParentInstitution()))
            .collect(Collectors.toSet());
    }

    private URI randomParentInstitution() {
        return randomElement(parentInstitutionsThatAreNvaCustomers);
    }

    private List<URI> smallSetOfParentInstitutions() {
        return intStream(10)
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .collect(Collectors.toList());
    }

    private List<CristinAffiliation> activeAndInactiveAffiliations() {
        return intStream(smallNumber())
            .map(ignored -> randomAffiliation(randomBoolean()))
            .collect(Collectors.toList());
    }

    private int smallNumber() {
        return 50;
    }

    private void createPersonWithOneActiveAffiliation() {
        var person = nextPerson();
        List<CristinAffiliation> affiliations = List.of(randomAffiliation(ACTIVE));
        createCristinRecord(person, affiliations);
        personWithOneActiveAffiliationAndNoInactiveAffiliations = person;
        createStubResponseForPerson(person);
    }

    private void createPersonWithActiveAndInactiveAffiliations() {
        var person = nextPerson();
        personWithActiveAndInactiveAffiliations = person;
        var affiliations = activeAndInactiveAffiliations();
        createCristinRecord(person, affiliations);
        createStubResponseForPerson(person);
    }

    private void createPersonWithNoActiveAffiliations() {
        var person = nextPerson();
        personWithNoActiveAffiliations = person;
        var affiliations = List.of(randomAffiliation(INACTIVE), randomAffiliation(INACTIVE));
        createCristinRecord(person, affiliations);
        createStubResponseForPerson(person);
    }

    private void createPersonWithManyActiveAffiliations() {
        var person = nextPerson();
        personWithManyActiveAffiliations = person;
        var affiliations = intStream(LARGE_NUMBER_TO_AVOID_TEST_SUCCESS_BY_LACK)
            .map(ignored -> randomAffiliation(ACTIVE))
            .collect(Collectors.toList());

        createCristinRecord(person, affiliations);
        createStubResponseForPerson(person);
    }

    private Stream<Integer> intStream(int range) {
        return IntStream.range(0, range).boxed();
    }

    private CristinPersonResponse createCristinRecord(NationalIdentityNumber person,
                                                      Collection<CristinAffiliation> affiliations) {
        var response = CristinPersonResponse.builder()
            .withNin(person)
            .withCristinId(randomPersonUri())
            .withAffiliations(new ArrayList<>(affiliations))
            .withLastName(randomString())
            .withFirstName(randomString())
            .build();
        cristinPersonRegistry.put(person, response);
        return response;
    }

    private void attachCristinOrgResponseToStub(PersonAffiliation personAffiliation) {
        stubFor(get(personAffiliation.getOrganization().getPath())
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .willReturn(aResponse().withStatus(HTTP_OK)
                                    .withBody(toCristinOrgResponse(personAffiliation).toString())));
    }

    private CristinOrgResponse toCristinOrgResponse(PersonAffiliation personAffiliation) {
        return CristinOrgResponse.create(personAffiliation.getOrganization(), personAffiliation.getParentInstitution());
    }

    private void createStubResponseForPerson(NationalIdentityNumber person) {
        stubFor(post("/person/identityNumber")
                    .withHeader(AUTHORIZATION_HEADER,
                                new EqualToPattern("Bearer " + dataporten.getJwtToken(), MATCH_CASE))
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .withRequestBody(cristinServiceRequestBody(person))
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cristinResponseBody(person))));
    }

    private ContentPattern<?> cristinServiceRequestBody(NationalIdentityNumber nin) {
        String jsonBody = String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.toString());
        return new EqualToJsonPattern(jsonBody, IGNORE_ARRAY_ORDER, DO_NOT_IGNORE_OTHER_ELEMENTS);
    }

    private String cristinResponseBody(NationalIdentityNumber nin) {
        return cristinPersonRegistry.get(nin).toString();
    }

    private CristinAffiliation randomAffiliation(boolean activeAffiliation) {
        return CristinAffiliation.builder()
            .withActive(activeAffiliation)
            .withOrganization(randomOrgFromTheAffiliationsPool())
            .build();
    }

    private NationalIdentityNumber nextPerson() {
        var person = people.stream()
            .filter(not(peopleMatchedToSomeScenario::contains))
            .findFirst()
            .orElseThrow();
        peopleMatchedToSomeScenario.add(person);
        return person;
    }

    private StringValuePattern applicationJson() {
        return new EqualToPattern("application/json", MATCH_CASE);
    }
}
