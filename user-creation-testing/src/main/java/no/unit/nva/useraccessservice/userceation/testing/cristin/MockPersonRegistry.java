package no.unit.nva.useraccessservice.userceation.testing.cristin;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitutionUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.util.Objects.nonNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static nva.commons.core.attempt.Try.attempt;

public class MockPersonRegistry {

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;
    private static final String HTTPS_SCHEME = "https";
    private static final String CRISTIN_PATH = "cristin";
    private static final String PERSON_PATH = "person";
    private static final String ORGANIZATION_PATH = "organization";
    private static final String AUTHORIZATION_HEADER_NAME = "authorization";
    private static final String PERSONS_RESOLVE_BY_NATIONAL_ID = "/persons/resolve?national_id=%s";
    private final Map<String, CristinPerson> ninToPeople;
    private final Map<String, CristinPerson> cristinIdToPeople;
    private final Map<String, URI> cristinInstitutionIdToUnitUriMap;
    private final String basicAuthorizationHeaderValue;
    private final HttpHeaders defaultRequestHeaders;
    private URI cristinBaseUri;

    public MockPersonRegistry(String username,
                              String password,
                              URI cristinBaseUri,
                              HttpHeaders defaultRequestHeaders) {
        this.ninToPeople = new ConcurrentHashMap<>();
        this.cristinIdToPeople = new ConcurrentHashMap<>();
        this.cristinInstitutionIdToUnitUriMap = new ConcurrentHashMap<>();
        this.cristinBaseUri = cristinBaseUri;
        this.basicAuthorizationHeaderValue = generateBasicAuthorizationHeaderValue(username, password);
        this.defaultRequestHeaders = defaultRequestHeaders;
    }

    private String generateBasicAuthorizationHeaderValue(String username, String password) {
        var toBeEncoded = (username + ":" + password).getBytes();
        return "Basic " + Base64.getEncoder().encodeToString(toBeEncoded);
    }

    public MockedPersonData mockResponseForBadGateway() {
        var nin = randomNin();
        var cristinId = randomString();
        createPersonSearchStubBadGateway(nin);
        return new MockedPersonData(nin, cristinId);
    }

    private void createPersonSearchStubBadGateway(String nin) {
        stubWithDefaultRequestHeadersEqualToFor(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }

    private void stubWithDefaultRequestHeadersEqualToFor(MappingBuilder mappingBuilder) {
        defaultRequestHeaders.stream()
            .forEach(entry -> mappingBuilder.withHeader(entry.getKey(), equalTo(entry.getValue())));

        stubFor(mappingBuilder);
    }

    public MockedPersonData mockResponseForIllegalJson() {
        var nin = randomNin();
        var cristinId = randomString();
        createPersonSearchStubIllegalJson(nin);
        return new MockedPersonData(nin, cristinId);
    }

    private void createPersonSearchStubIllegalJson(String nin) {
        var illegalBodyAsObjectIsExpected = "[]";

        stubWithDefaultRequestHeadersEqualToFor(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(illegalBodyAsObjectIsExpected)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    public MockedPersonData personWithoutAffiliations() {
        var nin = randomNin();
        var cristinId = randomString();


        createPersonWithoutAffiliations(nin, cristinId);

        return new MockedPersonData(nin, cristinId);
    }

    private void createPersonWithoutAffiliations(String nin, String cristinId) {

        var person = new CristinPerson(cristinId, randomString(), randomString(), null, null);
        var institutions = Collections.<String>emptyList();
        updateBuffersAndStubs(nin, person, institutions);
    }

    private void updateBuffersAndStubs(String nin,
                                       CristinPerson cristinPerson,
                                       List<String> institutionIds) {
        if (nonNull(nin)) {
            ninToPeople.put(nin, cristinPerson);
        }
        cristinIdToPeople.put(cristinPerson.getId(), cristinPerson);
        createStubsForPerson(nin, cristinPerson);
        institutionIds.forEach(this::createStubForInstitution);
    }

    private void createStubForInstitution(String institutionIdentifier) {
        var institutionUnitIdentifier = randomString();
        var urlForUnit = generateUnitUrl(institutionUnitIdentifier);
        var correspondingUnit = new CristinInstitutionUnit(institutionUnitIdentifier, urlForUnit);
        var cristinInstitution = new CristinInstitution(correspondingUnit);

        this.cristinInstitutionIdToUnitUriMap.put(institutionIdentifier,
            nvaScopedOrganizationCristinId(institutionUnitIdentifier));

        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinInstitution))
            .orElseThrow();

        stubWithDefaultRequestHeadersEqualToFor(
            get("/institutions/" + institutionIdentifier)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    private String generateUnitUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("units", identifier).getUri().toString();
    }

    private URI nvaScopedOrganizationCristinId(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, ServiceConstants.API_DOMAIN)
            .addChild(CRISTIN_PATH, ORGANIZATION_PATH, identifier)
            .getUri();
    }

    private void createStubsForPerson(String nin, CristinPerson cristinPerson) {
        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson))
            .orElseThrow();

        stubWithDefaultRequestHeadersEqualToFor(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
        
        stubWithDefaultRequestHeadersEqualToFor(
            get("/persons/" + cristinPerson.getId())
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    public void createPostPersonStub(CristinPerson cristinPerson) {
        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson))
                  .orElseThrow();

        stubWithDefaultRequestHeadersEqualToFor(
            post("/persons")
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                                .withBody(response)
                                .withStatus(HttpURLConnection.HTTP_OK)));
    }


    public MockedPersonData personWithExactlyOneActiveEmployment() {
        var nin = randomNin();
        var cristinId = randomString();

        var affiliations = List.of(createCristinAffiliation(ACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
    }

    private CristinAffiliation createCristinAffiliation(boolean active) {
        var institution = createAffiliationInstitution();
        var unit = createAffiliationUnit();

        return new CristinAffiliation(institution, unit, active);
    }

    private CristinAffiliationInstitution createAffiliationInstitution() {
        var identifier = randomString();
        return new CristinAffiliationInstitution(identifier, generateInstitutionUrl(identifier));
    }

    private String generateInstitutionUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("institutions", identifier).getUri().toString();
    }

    private CristinAffiliationUnit createAffiliationUnit() {
        var identifier = randomString();
        return new CristinAffiliationUnit(identifier, generateUnitUrl(identifier));
    }

    private void createPersonWithAffiliations(String nin,
                                              String cristinId,
                                              List<CristinAffiliation> cristinAffiliations) {
        var person = new CristinPerson(cristinId, randomString(), randomString(), cristinAffiliations, null);

        var institutions = cristinAffiliations.stream()
            .map(CristinAffiliation::getInstitution)
            .map(CristinAffiliationInstitution::getId)
            .collect(Collectors.toList());

        updateBuffersAndStubs(nin, person, institutions);
    }

    public MockedPersonData personWithExactlyOneInactiveEmployment() {
        var nin = randomNin();
        var cristinId = randomString();

        var affiliations = List.of(createCristinAffiliation(INACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
    }

    public MockedPersonData personWithoutNin() {
        var cristinId = randomString();

        createPersonWithoutAffiliations(null, cristinId);

        return new MockedPersonData(null, cristinId);
    }

    public MockedPersonData personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions() {
        var nin = randomNin();
        var cristinId = randomString();

        var affiliations = List.of(
            createCristinAffiliation(ACTIVE),
            createCristinAffiliation(INACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
    }

    public MockedPersonData personWithOneActiveAndOneInactiveEmploymentInSameInstitution() {
        var nin = randomNin();
        var cristinId = randomString();

        var activeAffiliation = createCristinAffiliation(ACTIVE);
        var affiliations = List.of(
            activeAffiliation,
            createCristinAffiliation(activeAffiliation.getInstitution(), INACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
    }

    public MockedPersonData personWithTwoActiveEmploymentsInDifferentInstitutions() {
        var nin = randomNin();
        var cristinId = randomString();

        var affiliations = List.of(
            createCristinAffiliation(ACTIVE),
            createCristinAffiliation(ACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
    }

    public URI getCristinIdForInstitution(String identifier) {
        return cristinInstitutionIdToUnitUriMap.get(identifier);
    }

    public URI getCristinIdForUnit(String identifier) {
        return nvaScopedOrganizationCristinId(identifier);
    }

    public void setCristinBaseUri(URI cristinBaseUri) {
        this.cristinBaseUri = cristinBaseUri;
    }

    public URI getCristinIdForPerson(String nin) {
        var person = getPerson(nin);
        return nvaScopedPersonCristinId(person.getId());
    }

    public CristinPerson getPerson(String nin) {
        return ninToPeople.get(nin);
    }

    private URI nvaScopedPersonCristinId(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, ServiceConstants.API_DOMAIN)
            .addChild(CRISTIN_PATH, PERSON_PATH, identifier)
            .getUri();
    }

    private CristinAffiliation createCristinAffiliation(CristinAffiliationInstitution institution, boolean active) {
        var unit = createAffiliationUnit();

        return new CristinAffiliation(institution, unit, active);
    }

    public MockedPersonData mockResponseForPersonNotFound() {
        var nin = randomNin();
        var cristinId = randomString();
        stubWithDefaultRequestHeadersEqualToFor(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_NOT_FOUND)));
        return new MockedPersonData(nin, cristinId);
    }
    
    public void setupServerErrorForNin(String nin) {
        stubWithDefaultRequestHeadersEqualToFor(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }
    
    public void setupCreatePersonAlreadyExistsError() {
        var errorBody = """
            {
                "status" : 400,
                "response_id" : "test123",
                 "errors" : [ "Norwegian national id already exists." ]
             }
            """;
        stubWithDefaultRequestHeadersEqualToFor(
            post("/persons")
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                                .withStatus(HttpURLConnection.HTTP_BAD_REQUEST)
                                .withBody(errorBody)));
    }

    public void setupCreatePersonConflictError() {
        var errorBody = """
            {
                "status" : 409,
                "response_id" : "test456",
                "errors" : [ "Conflict: Person already exists." ]
            }
            """;
        stubWithDefaultRequestHeadersEqualToFor(
            post("/persons")
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_CONFLICT)
                    .withBody(errorBody)));
    }

    public Set<URI> getUnitCristinUris(String nin) {
        return getUnitCristinUris(nin, allAffiliations());
    }

    private Set<URI> getUnitCristinUris(String nin, Predicate<CristinAffiliation> predicate) {
        var person = getPerson(nin);
        return person.getAffiliations().stream()
            .filter(predicate)
            .map(CristinAffiliation::getUnit)
            .map(CristinAffiliationUnit::getId)
            .distinct()
            .map(this::nvaScopedOrganizationCristinId)
            .collect(Collectors.toSet());
    }

    private Predicate<CristinAffiliation> allAffiliations() {
        return cristinAffiliation -> true;
    }

    public Set<URI> getInstitutionUnitCristinUris(String nin) {
        return getInstitutionUnitCristinUris(nin, allAffiliations());
    }

    private Set<URI> getInstitutionUnitCristinUris(String nin, Predicate<CristinAffiliation> predicate) {
        var person = getPerson(nin);
        return person.getAffiliations().stream()
            .filter(predicate)
            .map(CristinAffiliation::getInstitution)
            .map(CristinAffiliationInstitution::getId)
            .distinct()
            .map(this.cristinInstitutionIdToUnitUriMap::get)
            .collect(Collectors.toSet());
    }

    public Set<URI> getInstitutionUnitCristinUrisByState(String nin, boolean active) {
        return getInstitutionUnitCristinUris(nin, affiliationsByState(active));
    }

    private Predicate<CristinAffiliation> affiliationsByState(boolean active) {
        return cristinAffiliation -> cristinAffiliation.isActive() == active;
    }
}
