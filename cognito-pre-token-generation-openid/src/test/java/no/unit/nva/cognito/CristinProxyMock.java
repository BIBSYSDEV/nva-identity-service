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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinIdentifier;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import nva.commons.core.paths.UriWrapper;

public class CristinProxyMock {

    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final boolean INACTIVE = false;
    private static final Boolean MATCH_CASE = false;
    private static final boolean ACTIVE = true;
    private Set<NationalIdentityNumber> people;
    private final Set<NationalIdentityNumber> peopleMatchedToSomeScenario;
    private final DataportenMock dataporten;
    private final URI cristinPersonHost;
    private final Map<NationalIdentityNumber, CristinPersonResponse> cristinPersonRegistry;

    private NationalIdentityNumber personWithOneActiveAffiliationAndNoInactiveAffiliations;
    private NationalIdentityNumber personWithNoActiveAffiliations;
    private NationalIdentityNumber personWithActiveAndInactiveAffiliations;
    private List<URI> topLevelOrgUris;
    private Set<BottomAndTopLevelOrgPair> bottomLevelOrgs;
    private Set<URI> bottomLevelOrgUris;
    private Map<URI, URI> bottomTopLevelOrgMap;

    public CristinProxyMock(WireMockServer httpServer,
                            DataportenMock dataportenMock) {
        this.cristinPersonRegistry = new ConcurrentHashMap<>();
        peopleMatchedToSomeScenario = new HashSet<>();
        this.dataporten = dataportenMock;
        cristinPersonHost = URI.create(httpServer.baseUrl());
    }

    public NationalIdentityNumber getPersonWithActiveAndInactiveAffiliations() {
        return personWithActiveAndInactiveAffiliations;
    }

    public void initialize(Set<NationalIdentityNumber> people) {
        this.people = people;
        createImaginaryOrganizationStructure();
        createPersonWithOneActiveAffiliation();
        createPersonWithNoActiveAffiliations();
        createPersonWithActiveAndInactiveAffiliations();
    }

    public NationalIdentityNumber getPersonWithOneActiveAffiliationAndNoInactiveAffiliations() {
        return personWithOneActiveAffiliationAndNoInactiveAffiliations;
    }

    public List<URI> getTopLevelOrgUris() {
        return this.topLevelOrgUris;
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

    public URI randomOrgFromTheAffiliationsPool() {
        return randomElement(bottomLevelOrgUris);
    }

    public URI randomPersonUri() {
        return new UriWrapper(cristinPersonHost).addChild("person").addChild(randomString()).getUri();
    }

    public URI getTopLevelOrgForBottomLevelOrg(URI uri) {
        return bottomTopLevelOrgMap.get(uri);
    }

    private URI createRandomOrgUriForTheImaginarySetup() {
        return new UriWrapper(cristinPersonHost).addChild("organization").addChild(randomString()).getUri();
    }

    private void createImaginaryOrganizationStructure() {
        topLevelOrgUris = smallSetOfTopLevelOrgUris();
        bottomLevelOrgs = setOfBottomLevelOrgsSignificantlyBiggerThanTopLevelOrgSet();
        bottomLevelOrgs.forEach(this::attachCristinOrgResponseToStub);
        bottomLevelOrgUris = bottomLevelOrgs.stream().map(BottomAndTopLevelOrgPair::getBottomLevelOrg).collect(
            Collectors.toSet());
        bottomTopLevelOrgMap = bottomLevelOrgs.stream().collect(Collectors.toMap(
            BottomAndTopLevelOrgPair::getBottomLevelOrg, BottomAndTopLevelOrgPair::getTopLevelOrg));
    }

    private Set<BottomAndTopLevelOrgPair> setOfBottomLevelOrgsSignificantlyBiggerThanTopLevelOrgSet() {
        return IntStream.range(0, 20)
            .boxed()
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .map(bottomLevelOrgUri -> new BottomAndTopLevelOrgPair(bottomLevelOrgUri, randomElement(topLevelOrgUris)))
            .collect(Collectors.toSet());
    }

    private List<URI> smallSetOfTopLevelOrgUris() {
        return IntStream.range(0, 5).boxed()
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .collect(Collectors.toList());
    }

    private void createPersonWithActiveAndInactiveAffiliations() {
        var person = nextPerson();
        personWithActiveAndInactiveAffiliations = person;
        createCristinRecordForPersonWithActiveAndInactiveAffiliations(person);
        createStubResponseForPerson(person);
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

    private void createPersonWithOneActiveAffiliation() {
        var person = nextPerson();
        createCristinPersonResponseForPersonHavingOneActiveAffiliationAndNoInactiveOnes(person);
        personWithOneActiveAffiliationAndNoInactiveAffiliations = person;
        createStubResponseForPerson(person);
    }

    private void createPersonWithNoActiveAffiliations() {
        var person = nextPerson();
        personWithNoActiveAffiliations = person;
        createCristinResponseForUserWithNoActiveAffiliations(person);
        createStubResponseForPerson(person);
    }

    private void attachCristinOrgResponseToStub(BottomAndTopLevelOrgPair bottomOrganization) {
        stubFor(get(bottomOrganization.getBottomLevelOrg().getPath())
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .willReturn(aResponse().withStatus(HTTP_OK)
                                    .withBody(bottomOrganization.toCristinOrgResponse().toString())));
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
            .withAffiliations(List.of(randomAffiliation(ACTIVE)))
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
