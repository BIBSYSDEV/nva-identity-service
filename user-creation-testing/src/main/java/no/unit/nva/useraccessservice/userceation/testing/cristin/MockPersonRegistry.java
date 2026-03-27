package no.unit.nva.useraccessservice.userceation.testing.cristin;

import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliationUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;

public class MockPersonRegistry {

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = false;
    private static final String HTTPS_SCHEME = "https";
    private static final String CRISTIN_PATH = "cristin";
    private static final String PERSON_PATH = "person";
    private static final String ORGANIZATION_PATH = "organization";
    private final Map<String, CristinPerson> ninToPeople;
    private final Map<String, CristinPerson> cristinIdToPeople;
    private final Map<String, URI> cristinInstitutionIdToUnitUriMap;
    private final CristinWireMockStubs wireMockStubs;

    public MockPersonRegistry(String username,
                              String password,
                              URI cristinBaseUri,
                              HttpHeaders defaultRequestHeaders) {
        this.ninToPeople = new ConcurrentHashMap<>();
        this.cristinIdToPeople = new ConcurrentHashMap<>();
        this.cristinInstitutionIdToUnitUriMap = new ConcurrentHashMap<>();
        var basicAuthorizationHeaderValue = generateBasicAuthorizationHeaderValue(username, password);
        this.wireMockStubs = new CristinWireMockStubs(basicAuthorizationHeaderValue, cristinBaseUri,
            defaultRequestHeaders);
    }

    public MockedPersonData mockResponseForBadGateway() {
        var nin = randomNin();
        var cristinId = randomString();
        wireMockStubs.createPersonSearchStubBadGateway(nin);
        return new MockedPersonData(nin, cristinId);
    }

    public MockedPersonData mockResponseForIllegalJson() {
        var nin = randomNin();
        var cristinId = randomString();
        wireMockStubs.createPersonSearchStubIllegalJson(nin);
        return new MockedPersonData(nin, cristinId);
    }

    public MockedPersonData personWithoutAffiliations() {
        var nin = randomNin();
        var cristinId = randomString();

        createPersonWithoutAffiliations(nin, cristinId);

        return new MockedPersonData(nin, cristinId);
    }

    public void createPostPersonStub(CristinPerson cristinPerson) {
        wireMockStubs.createPostPersonStub(cristinPerson);
    }

    public MockedPersonData personWithExactlyOneActiveEmployment() {
        var nin = randomNin();
        var cristinId = randomString();

        var affiliations = List.of(createCristinAffiliation(ACTIVE));

        createPersonWithAffiliations(nin, cristinId, affiliations);

        return new MockedPersonData(nin, cristinId);
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
        wireMockStubs.setCristinBaseUri(cristinBaseUri);
    }

    public URI getCristinIdForPerson(String nin) {
        var person = getPerson(nin);
        return nvaScopedPersonCristinId(person.getId());
    }

    public CristinPerson getPerson(String nin) {
        return ninToPeople.get(nin);
    }

    public MockedPersonData mockResponseForPersonNotFound() {
        var nin = randomNin();
        var cristinId = randomString();
        wireMockStubs.createPersonNotFoundStub(nin);
        return new MockedPersonData(nin, cristinId);
    }

    public void setupServerErrorForNin(String nin) {
        wireMockStubs.createServerErrorStub(nin);
    }

    public void setupCreatePersonAlreadyExistsError() {
        wireMockStubs.createPersonAlreadyExistsStub();
    }

    public void setupCreatePersonConflictError() {
        wireMockStubs.createPersonConflictStub();
    }

    public MockedPersonData personWithActiveAffiliationAtRedirectingInstitution() {
        var nin = randomNin();
        var cristinId = randomString();
        var originalInstitutionId = randomString();
        var redirectedCorrespondingUnitId = randomString();

        var institution = new CristinAffiliationInstitution(originalInstitutionId,
            wireMockStubs.generateInstitutionUrl(originalInstitutionId));
        var unit = createAffiliationUnit();
        var affiliation = new CristinAffiliation(institution, unit, ACTIVE);

        var person = new CristinPerson(cristinId, randomString(), randomString(), List.of(affiliation), null);

        ninToPeople.put(nin, person);
        cristinIdToPeople.put(person.getId(), person);
        wireMockStubs.createStubsForPerson(nin, person);
        wireMockStubs.createStubForRedirectingInstitution(originalInstitutionId, redirectedCorrespondingUnitId);

        return new MockedPersonData(nin, cristinId);
    }

    public Set<URI> getUnitCristinUris(String nin) {
        return getUnitCristinUris(nin, allAffiliations());
    }

    public Set<URI> getInstitutionUnitCristinUris(String nin) {
        return getInstitutionUnitCristinUris(nin, allAffiliations());
    }

    public Set<URI> getInstitutionUnitCristinUrisByState(String nin, boolean active) {
        return getInstitutionUnitCristinUris(nin, affiliationsByState(active));
    }

    private static String generateBasicAuthorizationHeaderValue(String username, String password) {
        var toBeEncoded = (username + ":" + password).getBytes();
        return "Basic " + Base64.getEncoder().encodeToString(toBeEncoded);
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
        wireMockStubs.createStubsForPerson(nin, cristinPerson);
        institutionIds.forEach(this::createStubForInstitution);
    }

    private void createStubForInstitution(String institutionIdentifier) {
        var institutionUnitIdentifier = institutionIdentifier + ".0.0.0";
        this.cristinInstitutionIdToUnitUriMap.put(institutionIdentifier,
            nvaScopedOrganizationCristinId(institutionUnitIdentifier));
        wireMockStubs.createStubForInstitution(institutionIdentifier);
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
        return new CristinAffiliationInstitution(identifier, wireMockStubs.generateInstitutionUrl(identifier));
    }

    private CristinAffiliationUnit createAffiliationUnit() {
        var identifier = randomString();
        return new CristinAffiliationUnit(identifier, wireMockStubs.generateUnitUrl(identifier));
    }

    private void createPersonWithAffiliations(String nin,
                                              String cristinId,
                                              List<CristinAffiliation> cristinAffiliations) {
        var person = new CristinPerson(cristinId, randomString(), randomString(), cristinAffiliations, null);

        var institutions = cristinAffiliations.stream()
            .map(CristinAffiliation::getInstitution)
            .map(CristinAffiliationInstitution::getId)
            .toList();

        updateBuffersAndStubs(nin, person, institutions);
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

    private Predicate<CristinAffiliation> allAffiliations() {
        return cristinAffiliation -> true;
    }

    private Predicate<CristinAffiliation> affiliationsByState(boolean active) {
        return cristinAffiliation -> cristinAffiliation.isActive() == active;
    }
}
