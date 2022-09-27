package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

public class MockPersonRegistry {
    
    public static final boolean IGNORE_ARRAY_ORDER = true;
    private static final Boolean IGNORE_EXTRA_ELEMENTS = true;
    public static final boolean ACTIVE = true;
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    private static final boolean INACTIVE = false;
    
    private final String accessToken;
    private final URI hostUri;
    
    private Map<NationalIdentityNumber, CristinPersonResponse> people;
    private Map<NationalIdentityNumber, List<CristinOrgResponse>> employments;
    private Map<NationalIdentityNumber, List<URI>> topLevelOrgs;
    
    //TODO: remove customer service from MockPersonRegistry and re-organize data generation
    public MockPersonRegistry(String accessToken, URI hostUri) {
        people = new ConcurrentHashMap<>();
        employments = new ConcurrentHashMap<>();
        topLevelOrgs = new ConcurrentHashMap<>();
        this.accessToken = accessToken;
        this.hostUri = hostUri;
    }
    
    public NationalIdentityNumber newPerson() {
        var nin = newNin();
        var person = createPerson(nin);
        people.put(nin, person);
        createStubForPerson(nin);
        return nin;
    }
    
    public CristinPersonResponse getPerson(NationalIdentityNumber nin) {
        return people.get(nin);
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmployment() throws ConflictException, NotFoundException {
        var nin = newNin();
        var topLeveLOrganization = createTopLevelOrganization();
        var notTopLevelOrganization = createNonTopLevelOrganization(topLeveLOrganization);
        var person = createPersonWithExactlyOneEmployment(nin, notTopLevelOrganization, ACTIVE);
        people.put(nin, person);
        employments.put(nin, List.of(notTopLevelOrganization));
        topLevelOrgs.put(nin, List.of(topLeveLOrganization));
        createStubForPerson(nin);
        return nin;
    }
    
    public List<URI> fetchTopLevelOrgsForPerson(NationalIdentityNumber nin) {
        return topLevelOrgs.get(nin);
    }
    
    public NationalIdentityNumber personWithExactlyOneInactiveEmployment() {
        var nin = newNin();
        var topLeveLOrganization = createTopLevelOrganization();
        var notTopLevelOrganization = createNonTopLevelOrganization(topLeveLOrganization);
        var person = createPersonWithExactlyOneEmployment(nin, notTopLevelOrganization, INACTIVE);
        people.put(nin, person);
        employments.put(nin, List.of(notTopLevelOrganization));
        topLevelOrgs.put(nin, List.of(topLeveLOrganization));
        createStubForPerson(nin);
        return nin;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmployment() {
        var nin = newNin();
        var activeEmploymentTopLevelOrg = createTopLevelOrganization();
        var activeEmploymentNonTopLevelOrg = createNonTopLevelOrganization(activeEmploymentTopLevelOrg);
        var inactiveEmploymentTopLevelOrg = createTopLevelOrganization();
        var inactiveEmploymentNonTopLevelOrg = createNonTopLevelOrganization(inactiveEmploymentTopLevelOrg);
        var person = createPersonWithOneActiveAndOneInactiveEmployment(
            nin,
            activeEmploymentNonTopLevelOrg,
            inactiveEmploymentNonTopLevelOrg
        );
        people.put(nin, person);
        employments.put(nin, List.of(activeEmploymentNonTopLevelOrg, inactiveEmploymentNonTopLevelOrg));
        topLevelOrgs.put(nin, List.of(activeEmploymentTopLevelOrg, inactiveEmploymentTopLevelOrg));
        assertThat(person, doesNotHaveEmptyValues());
        return nin;
    }
    
    private CristinPersonResponse createPersonWithOneActiveAndOneInactiveEmployment(NationalIdentityNumber nin,
                                                                                    CristinOrgResponse activeEmploymentNonTopLevelOrg,
                                                                                    CristinOrgResponse inactiveEmploymentNonTopLevelOrg) {
        return CristinPersonResponse.builder()
                   .withNin(nin)
                   .withAffiliations(List.of(
                       createAffiliation(activeEmploymentNonTopLevelOrg, ACTIVE),
                       createAffiliation(inactiveEmploymentNonTopLevelOrg, INACTIVE)))
                   .withCristinId(randomUri())
                   .withFirstName(randomString())
                   .withLastName(randomString())
                   .build();
    }
    
    private CristinOrgResponse createNonTopLevelOrganization(URI topLevelOrg) {
        var leaf = randomOrgUri();
        var path = leaf.getPath();
        var organization = CristinOrgResponse.create(leaf, randomOrgUri(), randomOrgUri(), topLevelOrg);
        var orgJsonString = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(organization)).orElseThrow();
        stubFor(get(path)
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
                    .willReturn(aResponse()
                                    .withStatus(HttpURLConnection.HTTP_OK)
                                    .withBody(orgJsonString)));
        return organization;
    }
    
    private URI createTopLevelOrganization() {
        return randomOrgUri();
    }
    
    private URI randomOrgUri() {
        return UriWrapper.fromUri(hostUri).addChild("cristin", "organization", randomString()).getUri();
    }
    
    private CristinPersonResponse createPersonWithExactlyOneEmployment(NationalIdentityNumber nin,
                                                                       CristinOrgResponse employment,
                                                                       boolean active) {
        var person = CristinPersonResponse.builder()
                         .withNin(nin)
                         .withAffiliations(List.of(createAffiliation(employment, active)))
                         .withCristinId(randomUri())
                         .withFirstName(randomString())
                         .withLastName(randomString())
                         .build();
        
        assertThat(person, doesNotHaveEmptyValues());
        return person;
    }
    
    private CristinAffiliation createAffiliation(CristinOrgResponse employment, boolean active) {
        return CristinAffiliation.builder()
                   .withOrganization(employment.getOrgId())
                   .withActive(active)
                   .build();
    }
    
    private static ObjectNode createRequestBody(NationalIdentityNumber nin) {
        var requestBody = JsonUtils.dtoObjectMapper.createObjectNode();
        requestBody.put("type", "NationalIdentificationNumber");
        requestBody.put("value", nin.getNin());
        return requestBody;
    }
    
    private static NationalIdentityNumber newNin() {
        return new NationalIdentityNumber(randomString());
    }
    
    private void createStubForPerson(NationalIdentityNumber nin) {
        var requestBody = createRequestBody(nin);
        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(people.get(nin))).orElseThrow();
        
        stubFor(post("/cristin/person/identityNumber")
                    .withHeader("Authorization", equalTo("Bearer " + accessToken))
                    .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
                    .withRequestBody(new EqualToJsonPattern(requestBody, IGNORE_ARRAY_ORDER, IGNORE_EXTRA_ELEMENTS))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }
    
    private CristinPersonResponse createPerson(NationalIdentityNumber nin) {
        return CristinPersonResponse.builder()
                   .withNin(nin)
                   .withFirstName(randomString())
                   .withLastName(randomString())
                   .withCristinId(randomUri())
                   .withAffiliations(List.of(randomAffiliation()))
                   .build();
    }
    
    private CristinAffiliation randomAffiliation() {
        return randomAffiliation(randomBoolean());
    }
    
    private CristinAffiliation randomAffiliation(boolean active) {
        return CristinAffiliation.builder().withActive(active).withOrganization(randomUri()).build();
    }
}
