package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;

public class MockPersonRegistry {
    
    public static final boolean IGNORE_ARRAY_ORDER = true;
    private static final Boolean IGNORE_EXTRA_ELEMENTS = true;
    private final String accessToken;
    
    private Map<NationalIdentityNumber, CristinPersonResponse> people;
    
    public MockPersonRegistry(String accessToken) {
        people = new ConcurrentHashMap<>();
        this.accessToken = accessToken;
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
                    .withHeader("Content-Type", equalTo("application/json"))
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
        return CristinAffiliation.builder().withActive(randomBoolean()).withOrganization(randomUri()).build();
    }
}
