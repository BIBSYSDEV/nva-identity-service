package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.function.Predicate.not;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.CONTENT_TYPE;
import static no.unit.nva.cognito.cristin.person.CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.org.CristinOrgResponse;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.paths.UriWrapper;

public class CristinProxyMock {

    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final boolean INACTIVE = false;
    private static final Boolean MATCH_CASE = false;
    private static final boolean ACTIVE = true;
    private final Set<NationalIdentityNumber> people;
    private final CustomerService customerService;
    private final Set<NationalIdentityNumber> peopleMatchedToSomeScenario;
    private final DataportenMock dataporten;
    private final URI cristinPersonHost;
    private final Map<NationalIdentityNumber, CristinPersonResponse> cristinPersonRegistry;
    private final Map<NationalIdentityNumber, List<TopLevelOrg>> topOrgUris;
    private NationalIdentityNumber personWithOneActiveAffiliationAndNoInactiveAffiliations;
    private NationalIdentityNumber personWithNoActiveAffiliations;
    private NationalIdentityNumber personWithActiveAndInactiveAffiliations;

    public CristinProxyMock(WireMockServer httpServer,
                            DataportenMock dataportenMock,
                            Set<NationalIdentityNumber> people,
                            CustomerService customerService) {
        this.people = people;
        this.customerService = customerService;
        this.topOrgUris = new ConcurrentHashMap<>();
        this.cristinPersonRegistry = new ConcurrentHashMap<>();
        peopleMatchedToSomeScenario = new HashSet<>();
        this.dataporten = dataportenMock;
        cristinPersonHost = URI.create(httpServer.baseUrl());
    }

    public NationalIdentityNumber getPersonWithActiveAndInactiveAffiliations() {
        return personWithActiveAndInactiveAffiliations;
    }

    public void setup() {
        createPersonWithOneActiveAffiliation();
        createPersonWithNoActiveAffiliations();
        createPersonWithActiveAndInactiveAffiliations();
    }

    public NationalIdentityNumber getPersonWithOneActiveAffiliationAndNoInactiveAffiliations() {
        return personWithOneActiveAffiliationAndNoInactiveAffiliations;
    }

    public List<TopLevelOrg> getTopLevelOrgsForPerson(NationalIdentityNumber nin) {
        return this.topOrgUris.get(nin);
    }

    public Stream<TopLevelOrg> getTopLevelOrgs() {
        return this.topOrgUris.values().stream().flatMap(Collection::stream);
    }

    public CristinIdentifier getCristinPersonIdentifier(NationalIdentityNumber personLoggingIn) {
        return cristinPersonRegistry.get(personLoggingIn).getPersonsCristinIdentifier();
    }

    public NationalIdentityNumber getPersonWithOnlyInactiveAffiliations() {
        return personWithNoActiveAffiliations;
    }

    public CristinPersonResponse getCristinPersonRegistry(NationalIdentityNumber personLoggingIn) {
        return this.cristinPersonRegistry.get(personLoggingIn);
    }

    private void createPersonWithActiveAndInactiveAffiliations() {
        var person = nextPerson();
        personWithActiveAndInactiveAffiliations = person;
        CristinPersonResponse cristinPersonResponse =
            createCristinRecordForPersonWithActiveAndInactiveAffiliations(person);
        setupPersonAndOrgResponses(person, cristinPersonResponse);
    }

    private CristinPersonResponse createCristinRecordForPersonWithActiveAndInactiveAffiliations(
        NationalIdentityNumber person) {
        var cristinPersonResponse = CristinPersonResponse.builder()
            .withCristinId(randomPersonUri())
            .withNin(person)
            .withAffiliations(activeAndInactiveAffiliations())
            .withFirstName(randomString())
            .withLastName(randomString())
            .build();
        cristinPersonRegistry.put(person, cristinPersonResponse);
        return cristinPersonResponse;
    }

    private List<CristinAffiliation> activeAndInactiveAffiliations() {
        return IntStream.range(0, smallNumber()).boxed()
            .map(ignores -> randomAffiliation(randomBoolean()))
            .collect(Collectors.toList());
    }

    private int smallNumber() {
        return 2 + randomInteger(10);
    }

    private CristinPersonResponse createCristinResponseForUserWithNoActiveAffiliations(
        NationalIdentityNumber person) {
        CristinPersonResponse response = CristinPersonResponse.builder()
            .withNin(person)
            .withCristinId(randomPersonUri())
            .withAffiliations(List.of(randomAffiliation(INACTIVE), randomAffiliation(INACTIVE)))
            .withLastName(randomString())
            .withFirstName(randomString())
            .build();
        cristinPersonRegistry.put(person, response);
        return response;
    }

    public URI randomOrgUri() {
        return new UriWrapper(cristinPersonHost).addChild("organization").addChild(randomString()).getUri();
    }

    public URI randomPersonUri() {
        return new UriWrapper(cristinPersonHost).addChild("person").addChild(randomString()).getUri();
    }

    private void createPersonWithOneActiveAffiliation() {
        var person = nextPerson();
        var cristinPersonResponse =
            createCristinPersonResponseForPersonHavingOneActiveAffiliationAndNoInactiveOnes(person);
        personWithOneActiveAffiliationAndNoInactiveAffiliations = person;
        setupPersonAndOrgResponses(person, cristinPersonResponse);
    }

    private void setupPersonAndOrgResponses(NationalIdentityNumber person,
                                            CristinPersonResponse cristinPersonResponse) {
        createStubResponseForPerson(person);
        createOrganizationStructureForPersonOrganizations(cristinPersonResponse);
    }

    private void createPersonWithNoActiveAffiliations() {
        var person = nextPerson();
        personWithNoActiveAffiliations = person;
        var cristinPersonResponse = createCristinResponseForUserWithNoActiveAffiliations(person);
        setupPersonAndOrgResponses(person, cristinPersonResponse);
    }

    private void createOrganizationStructureForPersonOrganizations(CristinPersonResponse cristinPersonResponse) {
        var cristinOrgStructure = cristinPersonResponse.getAffiliations()
            .stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::createRandomOrgStructureForOrg)
            .collect(Collectors.toList());
        var topOrgUris = cristinOrgStructure.stream()
            .map(cristinOrgResponse -> TopLevelOrg.create(cristinOrgResponse, cristinPersonResponse))
            .collect(Collectors.toList());

        this.topOrgUris.put(new NationalIdentityNumber(cristinPersonResponse.getNin()), topOrgUris);
        cristinOrgStructure.forEach(this::createCristinProxyResponseForOrg);
    }

    private URI randomTopLevelOrgUri() {
        var allCustomers = customerService.getCustomers().stream()
            .map(CustomerDto::getCristinId)
            .map(URI::create)
            .collect(Collectors.toList());
        return randomElement(allCustomers);
    }

    private void createCristinProxyResponseForOrg(CristinOrgResponse org) {
        stubFor(get(URI.create(org.getOrgId()).getPath())
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(org.toString())));
    }

    private CristinOrgResponse createRandomOrgStructureForOrg(URI uri) {
        return CristinOrgResponse.create(uri, randomTopLevelOrgUri());
    }

    private void createStubResponseForPerson(NationalIdentityNumber person) {
        stubFor(post("/person/identityNumber")
                    .withHeader(AUTHORIZATION_HEADER,
                                new EqualToPattern("Bearer " + dataporten.getJwtToken(), MATCH_CASE))
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .withRequestBody(cristinServiceRequestBody(person))
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cristinResponseBody(person))));
    }

    private CristinPersonResponse createCristinPersonResponseForPersonHavingOneActiveAffiliationAndNoInactiveOnes(
        NationalIdentityNumber nin) {
        var response = CristinPersonResponse.builder()
            .withCristinId(randomPersonUri())
            .withFirstName(randomString())
            .withLastName(randomString())
            .withNin(nin)
            .withAffiliations(List.of(randomAffiliation(true)))
            .build();
        this.cristinPersonRegistry.put(nin, response);
        return response;
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
            .withOrganization(randomOrgUri())
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
