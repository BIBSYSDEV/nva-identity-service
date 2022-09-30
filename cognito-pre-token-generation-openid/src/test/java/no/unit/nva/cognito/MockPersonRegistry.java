package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class MockPersonRegistry {
    
    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean ACTIVE = true;
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    private static final Boolean IGNORE_EXTRA_ELEMENTS = true;
    private static final boolean INACTIVE = false;
    
    private final String accessToken;
    private final URI hostUri;
    
    private final Map<NationalIdentityNumber, CristinPersonResponse> people;
    private final Map<NationalIdentityNumber, List<CristinOrgResponse>> employments;
    private final Map<NationalIdentityNumber, List<EmploymentInformation>> topLevelOrgs;
    private final Map<URI, URI> nonTopLevelOrgToTopLevelOrg;
    
    public MockPersonRegistry(String accessToken, URI hostUri) {
        people = new ConcurrentHashMap<>();
        employments = new ConcurrentHashMap<>();
        topLevelOrgs = new ConcurrentHashMap<>();
        nonTopLevelOrgToTopLevelOrg = new ConcurrentHashMap<>();
        this.accessToken = accessToken;
        this.hostUri = hostUri;
    }
    
    public CristinPersonResponse getPerson(NationalIdentityNumber nin) {
        return people.get(nin);
    }
    
    public NationalIdentityNumber personWithExactlyOneActiveEmployment() {
        var nin = new NationalIdentityNumber(randomString());
        var topLeveLOrganization = createTopLevelOrganization();
        var notTopLevelOrganization = createNonTopLevelOrganization(topLeveLOrganization);
        createPersonWithExactlyOneEmployment(nin, notTopLevelOrganization, ACTIVE);
        employments.put(nin, List.of(notTopLevelOrganization));
        topLevelOrgs.put(nin, List.of(new EmploymentInformation(topLeveLOrganization, ACTIVE)));
        nonTopLevelOrgToTopLevelOrg.put(notTopLevelOrganization.getOrgId(), topLeveLOrganization);
        return nin;
    }
    
    public NationalIdentityNumber personWithExactlyOneInactiveEmployment() {
        var nin = new NationalIdentityNumber(randomString());
        var topLeveLOrganization = createTopLevelOrganization();
        var notTopLevelOrganization = createNonTopLevelOrganization(topLeveLOrganization);
        createPersonWithExactlyOneEmployment(nin, notTopLevelOrganization, INACTIVE);
    
        employments.put(nin, List.of(notTopLevelOrganization));
        topLevelOrgs.put(nin, List.of(new EmploymentInformation(topLeveLOrganization, INACTIVE)));
        nonTopLevelOrgToTopLevelOrg.put(notTopLevelOrganization.getOrgId(), topLeveLOrganization);
        return nin;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmploymentInDifferentOrgs() {
        var nin = new NationalIdentityNumber(randomString());
        var activeEmploymentTopLevelOrg = createTopLevelOrganization();
        var activeEmploymentNonTopLevelOrg = createNonTopLevelOrganization(activeEmploymentTopLevelOrg);
        var inactiveEmploymentTopLevelOrg = createTopLevelOrganization();
        var inactiveEmploymentNonTopLevelOrg = createNonTopLevelOrganization(inactiveEmploymentTopLevelOrg);
        var person = createPersonWithOneActiveAndOneInactiveEmployment(
            nin,
            activeEmploymentNonTopLevelOrg,
            inactiveEmploymentNonTopLevelOrg
        );
        
        employments.put(nin, List.of(activeEmploymentNonTopLevelOrg, inactiveEmploymentNonTopLevelOrg));
        topLevelOrgs.put(nin, List.of(
            new EmploymentInformation(activeEmploymentTopLevelOrg, ACTIVE),
            new EmploymentInformation(inactiveEmploymentTopLevelOrg, INACTIVE)
        ));
        nonTopLevelOrgToTopLevelOrg.put(activeEmploymentNonTopLevelOrg.getOrgId(), activeEmploymentTopLevelOrg);
        nonTopLevelOrgToTopLevelOrg.put(inactiveEmploymentNonTopLevelOrg.getOrgId(), inactiveEmploymentTopLevelOrg);
        assertThat(person, doesNotHaveEmptyValues());
        return nin;
    }
    
    public NationalIdentityNumber personWithOneActiveAndOneInactiveEmploymentInSameOrg() {
        var nin = new NationalIdentityNumber(randomString());
        var topLevelOrg = createTopLevelOrganization();
        var activeEmploymentNonTopLevelOrg = createNonTopLevelOrganization(topLevelOrg);
        var inactiveEmploymentNonTopLevelOrg = createNonTopLevelOrganization(topLevelOrg);
        var person = createPersonWithOneActiveAndOneInactiveEmployment(
            nin,
            activeEmploymentNonTopLevelOrg,
            inactiveEmploymentNonTopLevelOrg
        );
        
        employments.put(nin, List.of(activeEmploymentNonTopLevelOrg, inactiveEmploymentNonTopLevelOrg));
        topLevelOrgs.put(nin, List.of(
            new EmploymentInformation(topLevelOrg, ACTIVE),
            new EmploymentInformation(topLevelOrg, INACTIVE)
        ));
    
        nonTopLevelOrgToTopLevelOrg.put(activeEmploymentNonTopLevelOrg.getOrgId(), topLevelOrg);
        nonTopLevelOrgToTopLevelOrg.put(activeEmploymentNonTopLevelOrg.getOrgId(), topLevelOrg);
    
        assertThat(person, doesNotHaveEmptyValues());
        return nin;
    }
    
    public List<EmploymentInformation> fetchTopOrgEmploymentInformation(NationalIdentityNumber nin) {
        return topLevelOrgs.get(nin);
    }
    
    public URI getTopLevelOrgForNonTopLevelOrg(URI organizationUri) {
        return nonTopLevelOrgToTopLevelOrg.get(organizationUri);
    }
    
    private static ObjectNode createRequestBody(NationalIdentityNumber nin) {
        var requestBody = JsonUtils.dtoObjectMapper.createObjectNode();
        requestBody.put("type", "NationalIdentificationNumber");
        requestBody.put("value", nin.getNin());
        return requestBody;
    }
    
    private CristinPersonResponse createPersonWithOneActiveAndOneInactiveEmployment(NationalIdentityNumber nin,
                                                                                    CristinOrgResponse activeEmploymentNonTopLevelOrg,
                                                                                    CristinOrgResponse inactiveEmploymentNonTopLevelOrg) {
        var response = CristinPersonResponse.builder()
                           .withNin(nin)
                           .withAffiliations(List.of(
                               createAffiliation(activeEmploymentNonTopLevelOrg, ACTIVE),
                               createAffiliation(inactiveEmploymentNonTopLevelOrg, INACTIVE)))
                           .withCristinId(randomUri())
                           .withFirstName(randomString())
                           .withLastName(randomString())
                           .build();
        return updateBuffersAndStubs(nin, response);
    }
    
    private CristinPersonResponse updateBuffersAndStubs(NationalIdentityNumber nin, CristinPersonResponse response) {
        people.put(nin, response);
        createStubForPerson(nin);
        return people.get(nin);
    }
    
    private CristinOrgResponse createNonTopLevelOrganization(URI topLevelOrg) {
        var leaf = randomOrgUri();
        var organization = CristinOrgResponse.create(leaf, randomOrgUri(), randomOrgUri(), topLevelOrg);
        var orgJsonString = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(organization)).orElseThrow();
        var path = leaf.getPath();
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
        return updateBuffersAndStubs(nin, person);
    }
    
    private CristinAffiliation createAffiliation(CristinOrgResponse employment, boolean active) {
        return CristinAffiliation.builder()
                   .withOrganization(employment.getOrgId())
                   .withActive(active)
                   .build();
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
    
    public static class EmploymentInformation {
        
        private final URI topLevelOrg;
        private final boolean active;
        
        public EmploymentInformation(URI topLevelOrg, boolean active) {
            this.topLevelOrg = topLevelOrg;
            this.active = active;
        }
        
        public URI getTopLevelOrg() {
            return topLevelOrg;
        }
        
        public boolean isActive() {
            return active;
        }
        
        @Override
        @JacocoGenerated
        public int hashCode() {
            return Objects.hash(getTopLevelOrg(), isActive());
        }
        
        @Override
        @JacocoGenerated
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EmploymentInformation)) {
                return false;
            }
            EmploymentInformation that = (EmploymentInformation) o;
            return isActive() == that.isActive() && Objects.equals(getTopLevelOrg(), that.getTopLevelOrg());
        }
    }
}
