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
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import nva.commons.core.paths.UriWrapper;

public class CristinProxyMock {

    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final boolean INACTIVE = false;
    public static final int LARGE_NUBMER_TO_AVOID_TEST_SUCESS_BY_LACK = 100;
    private static final Boolean MATCH_CASE = false;
    private static final boolean ACTIVE = true;
    private final Set<NationalIdentityNumber> peopleMatchedToSomeScenario;
    private final DataportenMock dataporten;
    private final URI cristinPersonHost;
    private final Map<NationalIdentityNumber, CristinPersonResponse> cristinPersonRegistry;
    private Set<NationalIdentityNumber> people;
    private NationalIdentityNumber personWithOneActiveAffiliationAndNoInactiveAffiliations;
    private NationalIdentityNumber personWithNoActiveAffiliations;
    private NationalIdentityNumber personWithActiveAndInactiveAffiliations;
    private NationalIdentityNumber personWithManyActiveAffiliations;
    private NationalIdentityNumber personWithActiveAffiliationThatIsNotCustomer;
    private List<URI> topLevelOrgUrisAndNvaCustomers;
    private Set<BottomAndTopLevelOrgPair> bottomLevelOrgs;
    private Set<URI> bottomLevelOrgUris;
    private Map<URI, URI> bottomTopLevelOrgMap;
    private URI topLevelOrgUriNotNvaCustomer;

    public CristinProxyMock(WireMockServer httpServer,
                            DataportenMock dataportenMock) {
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

    public List<URI> getTopLevelOrgUrisAndNvaCustomers() {
        return this.topLevelOrgUrisAndNvaCustomers;
    }

    public NationalIdentityNumber getPersonWithOnlyInactiveAffiliations() {
        return personWithNoActiveAffiliations;
    }

    public CristinPersonResponse getCristinPersonRecord(NationalIdentityNumber personLoggingIn) {
        return this.cristinPersonRegistry.get(personLoggingIn);
    }

    public URI randomOrgFromTheAffiliationsPool() {
        return randomElement(bottomLevelOrgUris);
    }

    public URI randomPersonUri() {
        return UriWrapper.fromUri(cristinPersonHost).addChild("person").addChild(randomString()).getUri();
    }

    public URI getTopLevelOrgForBottomLevelOrg(URI uri) {
        return bottomTopLevelOrgMap.get(uri);
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

    private URI getTopLevelOrgUriNotNvaCustomer() {
        return topLevelOrgUriNotNvaCustomer;
    }

    private void createPersonWithActiveAffiliationThatIsNotCustomer() {
        personWithActiveAffiliationThatIsNotCustomer = nextPerson();
        var affiliationThatIsNotCustomer = CristinAffiliation.builder()
            .withActive(ACTIVE)
            .withOrganization(getTopLevelOrgUriNotNvaCustomer())
            .build();
        createCristinRecord(personWithActiveAffiliationThatIsNotCustomer, List.of(affiliationThatIsNotCustomer));
        createStubResponseForPerson(personWithActiveAffiliationThatIsNotCustomer);
    }

    private URI createRandomOrgUriForTheImaginarySetup() {
        return UriWrapper.fromUri(cristinPersonHost).addChild("organization").addChild(randomString()).getUri();
    }

    private void createImaginaryOrganizationStructure() {
        createTopLevelOrgsThatAreGoingToBeNvaCustomers();
        createTopLeveOrgThatIsNotACustomer();
    }

    private void createTopLevelOrgsThatAreGoingToBeNvaCustomers() {
        topLevelOrgUrisAndNvaCustomers = smallSetOfTopLevelOrgUris();
        bottomLevelOrgs = setOfBottomLevelOrgsSignificantlyBiggerThanTopLevelOrgSet();
        bottomLevelOrgs.forEach(this::attachCristinOrgResponseToStub);
        bottomLevelOrgUris = bottomLevelOrgs.stream().map(BottomAndTopLevelOrgPair::getBottomLevelOrg).collect(
            Collectors.toSet());
        bottomTopLevelOrgMap = new HashMap<>();
        bottomLevelOrgs.forEach(entry->bottomTopLevelOrgMap.put(entry.getBottomLevelOrg(), entry.getTopLevelOrg()));
    }

    private void createTopLeveOrgThatIsNotACustomer() {
        topLevelOrgUriNotNvaCustomer = createRandomOrgUriForTheImaginarySetup();
        bottomTopLevelOrgMap.put(topLevelOrgUriNotNvaCustomer,topLevelOrgUriNotNvaCustomer);
        attachCristinOrgResponseToStub(new BottomAndTopLevelOrgPair(topLevelOrgUriNotNvaCustomer,topLevelOrgUriNotNvaCustomer));
    }

    private Set<BottomAndTopLevelOrgPair> setOfBottomLevelOrgsSignificantlyBiggerThanTopLevelOrgSet() {
        return IntStream.range(0, 20)
            .boxed()
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .map(bottomLevelOrgUri -> new BottomAndTopLevelOrgPair(bottomLevelOrgUri, randomElement(
                topLevelOrgUrisAndNvaCustomers)))
            .collect(Collectors.toSet());
    }

    private List<URI> smallSetOfTopLevelOrgUris() {
        return IntStream.range(0, 5).boxed()
            .map(ignored -> createRandomOrgUriForTheImaginarySetup())
            .collect(Collectors.toList());
    }

    private List<CristinAffiliation> activeAndInactiveAffiliations() {
        return IntStream.range(0, smallNumber()).boxed()
            .map(ignores -> randomAffiliation(randomBoolean()))
            .collect(Collectors.toList());
    }

    private int smallNumber() {
        return 2 + randomInteger(10);
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
        var affiliations = IntStream.range(0, LARGE_NUBMER_TO_AVOID_TEST_SUCESS_BY_LACK)
            .boxed()
            .map(ignored -> randomAffiliation(ACTIVE))
            .collect(Collectors.toList());

        createCristinRecord(person, affiliations);
        createStubResponseForPerson(person);
    }

    private CristinPersonResponse createCristinRecord(NationalIdentityNumber person,
                                                      Collection<CristinAffiliation> affiliations) {
        CristinPersonResponse response = CristinPersonResponse.builder()
            .withNin(person)
            .withCristinId(randomPersonUri())
            .withAffiliations(new ArrayList<>(affiliations))
            .withLastName(randomString())
            .withFirstName(randomString())
            .build();
        cristinPersonRegistry.put(person, response);
        return response;
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
