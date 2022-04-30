package no.unit.nva.useraccessservice.usercreation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.paths.UriWrapper;

public class CristinServerMock {

    public static final String ORGANIZATION_PATH = "organization";
    public static final String PERSON_IDENTITY_NUMBER_PATH = "/person/identityNumber";
    public static final String PERSON_PATH = "person";
    private URI serverUri;
    private WireMockServer httpServer;
    private Map<NationalIdentityNumber, CristinPersonResponse> people;

    public CristinServerMock() {
        people = new ConcurrentHashMap<>();
        setUpWiremock();
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void addPerson(NationalIdentityNumber nin, PersonAffiliation... employments) {
        for (var employment : employments) {
            createResponseForOrganization(employment);
        }
        var personCristinEntry = cristinPersonResponse(nin, employments);
        cacheCristinPersonResponsesForTestingAssertions(nin, personCristinEntry);
        addResponseToRegistryServer(nin, personCristinEntry);
    }

    private void createResponseForOrganization(PersonAffiliation orgStructure) {
        stubFor(WireMock.get(urlEqualTo(organizationPath(orgStructure.getChild())))
                    .willReturn(createInstitutionRegistryResponseForOrganization(orgStructure))

        );
    }

    private CristinPersonResponse cristinPersonResponse(NationalIdentityNumber nin,
                                                        PersonAffiliation... employmentAffiliations) {
        var affiliations = Arrays.stream(employmentAffiliations)
            .map(PersonAffiliation::toCristinAffiliation)
            .collect(Collectors.toList());
        return CristinPersonResponse.builder()
            .withCristinId(randomPersonId())
            .withAffiliations(affiliations)
            .withNin(nin)
            .build();
    }

    private void cacheCristinPersonResponsesForTestingAssertions(NationalIdentityNumber nin,
                                                                 CristinPersonResponse personCristinEntry) {
        people.put(nin, personCristinEntry);
    }

    private void addResponseToRegistryServer(NationalIdentityNumber nin, CristinPersonResponse personCristinEntry) {
        stubFor(post(PERSON_IDENTITY_NUMBER_PATH)
                    .withRequestBody(equalToJson(formatSearchByNinRequestBody(nin)))
                    .willReturn(aResponse().withBody(personCristinEntry.toString())));
    }

    public URI randomOrgUri() {
        return UriWrapper.fromUri(serverUri).addChild(ORGANIZATION_PATH).addChild(randomString()).getUri();
    }

    public URI getCristinId(NationalIdentityNumber person) {
        return people.get(person).getCristinId();
    }

    public void shutDown() {
        httpServer.stop();
    }

    private URI randomPersonId() {
        return UriWrapper.fromUri(serverUri).addChild(PERSON_PATH).addChild(randomString()).getUri();
    }

    private String formatSearchByNinRequestBody(NationalIdentityNumber nin) {
        return String.format(CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
    }

    private ResponseDefinitionBuilder createInstitutionRegistryResponseForOrganization(
        PersonAffiliation employmentAffiliations) {
        return aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
            .withBody(organizationBody(employmentAffiliations));
    }

    private String organizationBody(PersonAffiliation employmentAffiliation) {
        var parent = new CristinOrgResponse();
        parent.setOrgId(employmentAffiliation.getParent());
        var response = new CristinOrgResponse();
        response.setOrgId(employmentAffiliation.getChild());
        response.setPartOf(List.of(parent));
        return response.toString();
    }

    private String organizationPath(URI organization) {
        return UriWrapper.fromUri(organization).getPath().toString();
    }

    private void setUpWiremock() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }
}
