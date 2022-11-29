package no.unit.nva.useraccessservice.userceation.testing.cristin;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
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
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitutionUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.PersonSearchResultItem;
import nva.commons.core.paths.UriWrapper;

public class MockPersonRegistry {

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;
    private static final String HTTPS_SCHEME = "https";
    private static final String CRISTIN_PATH = "cristin";
    private static final String PERSON_PATH = "person";
    private static final String ORGANIZATION_PATH = "organization";
    private static final String AUTHORIZATION_HEADER_NAME = "authorization";
    private URI cristinBaseUri;
    private final Map<String, CristinPerson> people;
    private final Map<String, URI> cristinInstitutionIdToUnitUriMap;
    private final String basicAuthorizationHeaderValue;

    public MockPersonRegistry(String username, String password, URI cristinBaseUri) {
        this.people = new ConcurrentHashMap<>();
        this.cristinInstitutionIdToUnitUriMap = new ConcurrentHashMap<>();
        this.cristinBaseUri = cristinBaseUri;
        this.basicAuthorizationHeaderValue = generateBasicAuthorizationHeaderValue(username, password);
    }

    public String mockResponseForBadGateway() {
        var nin = randomString();
        createPersonSearchStubBadGateway(nin);
        return nin;
    }

    public String mockResponseForIllegalJson() {
        var nin = randomString();
        createPersonSearchStubIllegalJson(nin);
        return nin;
    }

    private String generateBasicAuthorizationHeaderValue(String username, String password) {
        var toBeEncoded = (username + ":" + password).getBytes();
        return "Basic " + Base64.getEncoder().encodeToString(toBeEncoded);
    }

    public CristinPerson getPerson(String nin) {
        return people.get(nin);
    }

    public String personWithoutAffiliations() {
        var nin = randomString();

        createPersonWithoutAffiliations(nin);

        return nin;
    }

    public String personWithExactlyOneActiveEmployment() {
        var nin = randomString();

        var affiliations = List.of(createCristinAffiliation(ACTIVE));

        createPersonWithAffiliations(nin, affiliations);

        return nin;
    }

    public String personWithExactlyOneInactiveEmployment() {
        var nin = randomString();

        var affiliations = List.of(createCristinAffiliation(INACTIVE));

        createPersonWithAffiliations(nin, affiliations);

        return nin;
    }

    public String personWithOneActiveAndOneInactiveEmploymentInDifferentInstitutions() {
        var nin = randomString();

        var affiliations = List.of(
            createCristinAffiliation(ACTIVE),
            createCristinAffiliation(INACTIVE));

        createPersonWithAffiliations(nin, affiliations);

        return nin;
    }

    public String personWithOneActiveAndOneInactiveEmploymentInSameInstitution() {
        var nin = randomString();

        var activeAffiliation = createCristinAffiliation(ACTIVE);
        var affiliations = List.of(
            activeAffiliation,
            createCristinAffiliation(activeAffiliation.getInstitution(), INACTIVE));

        createPersonWithAffiliations(nin, affiliations);

        return nin;
    }

    public String personWithTwoActiveEmploymentsInDifferentInstitutions() {
        var nin = randomString();

        var affiliations = List.of(
            createCristinAffiliation(ACTIVE),
            createCristinAffiliation(ACTIVE));

        createPersonWithAffiliations(nin, affiliations);

        return nin;
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

    private CristinAffiliation createCristinAffiliation(boolean active) {
        var institution = createAffiliationInstitution();
        var unit = createAffiliationUnit();

        return new CristinAffiliation(institution, unit, active);
    }

    private CristinAffiliation createCristinAffiliation(CristinAffiliationInstitution institution, boolean active) {
        var unit = createAffiliationUnit();

        return new CristinAffiliation(institution, unit, active);
    }

    private CristinAffiliationInstitution createAffiliationInstitution() {
        var identifier = randomString();
        return new CristinAffiliationInstitution(identifier, generateInstitutionUrl(identifier));
    }

    private CristinAffiliationUnit createAffiliationUnit() {
        var identifier = randomString();
        return new CristinAffiliationUnit(identifier, generateUnitUrl(identifier));
    }

    private String generateUnitUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("units", identifier).getUri().toString();
    }

    private String generateInstitutionUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("institutions", identifier).getUri().toString();
    }

    private void createPersonWithoutAffiliations(String nin) {
        var person = new CristinPerson(randomString(), randomString(), randomString(), null);

        var institutions = Collections.<String>emptyList();
        updateBuffersAndStubs(nin, person, institutions);
    }

    private void createPersonWithAffiliations(String nin,
                                              List<CristinAffiliation> cristinAffiliations) {
        var person = new CristinPerson(randomString(), randomString(), randomString(), cristinAffiliations);

        var institutions = cristinAffiliations.stream()
                               .map(CristinAffiliation::getInstitution)
                               .map(CristinAffiliationInstitution::getId)
                               .collect(Collectors.toList());

        updateBuffersAndStubs(nin, person, institutions);
    }

    private String generateCristinPersonUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("persons", identifier).getUri().toString();
    }

    private void updateBuffersAndStubs(String nin,
                                       CristinPerson cristinPerson,
                                       List<String> institutionIds) {
        people.put(nin, cristinPerson);
        createStubsForPerson(nin, cristinPerson);
        institutionIds.forEach(this::createStubForInstitution);
    }

    private URI nvaScopedOrganizationCristinId(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, ServiceConstants.API_DOMAIN)
                   .addChild(CRISTIN_PATH, ORGANIZATION_PATH, identifier)
                   .getUri();
    }

    private URI nvaScopedPersonCristinId(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, ServiceConstants.API_DOMAIN)
                   .addChild(CRISTIN_PATH, PERSON_PATH, identifier)
                   .getUri();
    }

    private void createStubForInstitution(String institutionIdentifier) {
        var institutionUnitIdentifier = randomString();
        //institutionUnitCristinUris.put(nin, List.of(nvaScopedCristinId(institutionUnitIdentifier)));
        var urlForUnit = generateUnitUrl(institutionUnitIdentifier);
        var correspondingUnit = new CristinInstitutionUnit(institutionUnitIdentifier, urlForUnit);
        var cristinInstitution = new CristinInstitution(correspondingUnit);

        this.cristinInstitutionIdToUnitUriMap.put(institutionIdentifier,
                                                  nvaScopedOrganizationCristinId(institutionUnitIdentifier));

        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinInstitution))
                  .orElseThrow();

        stubFor(get("/institutions/" + institutionIdentifier)
                    .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void createStubsForPerson(String nin, CristinPerson cristinPerson) {
        var personIdentifier = cristinPerson.getId();
        var personSearchResult = List.of(new PersonSearchResultItem(personIdentifier,
                                                                    generateCristinPersonUrl(personIdentifier)));
        createPersonSearchStub(nin, personSearchResult);
        createPersonGetStub(cristinPerson);
    }

    public String mockResponseForPersonNotFound() {
        var nin = randomString();
        createPersonSearchStub(nin, Collections.emptyList());
        return nin;
    }

    private void createPersonSearchStub(String nin, List<PersonSearchResultItem> searchResults) {
        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(searchResults))
                  .orElseThrow();

        stubFor(get("/persons?national_id=" + nin)
                    .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void createPersonSearchStubBadGateway(String nin) {

        stubFor(get("/persons?national_id=" + nin)
                    .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }

    private void createPersonSearchStubIllegalJson(String nin) {
        var illegalBodyAsArrayIsExpected = "{}";
        stubFor(get("/persons?national_id=" + nin)
                    .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                    .willReturn(aResponse()
                                    .withBody(illegalBodyAsArrayIsExpected)
                                    .withStatus(HttpURLConnection.HTTP_OK)));

    }

    private void createPersonGetStub(CristinPerson cristinPerson) {
        var response
            = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson))
                  .orElseThrow();

        stubFor(get("/persons/" + cristinPerson.getId())
                    .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
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

    private Predicate<CristinAffiliation> allAffiliations() {
        return cristinAffiliation -> true;
    }

    private Predicate<CristinAffiliation> affiliationsByState(boolean active) {
        return cristinAffiliation -> cristinAffiliation.isActive() == active;
    }
}
