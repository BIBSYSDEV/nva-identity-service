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
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.PersonAndInstitutionRegistryClient;
import nva.commons.core.paths.UriWrapper;

public class CristinServerMock {

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

    public void addPerson(NationalIdentityNumber nin, PersonEmployment... employmentAffiliations) {
        for (var employmentAffiliation : employmentAffiliations) {
            createResponseForOrganization(employmentAffiliation);
        }
        var personCristinEntry = cristinPersonResponse(nin, employmentAffiliations);
        cacheResponseForAssertions(nin, personCristinEntry);
        addResponseToRegistryServer(nin, personCristinEntry);
    }

    public URI randomOrgUri() {
        return UriWrapper.fromUri(serverUri).addChild("organization").addChild(randomString()).getUri();
    }

    public URI getCristinId(NationalIdentityNumber person) {
        return people.get(person).getCristinId();
    }

    public void shutDown() {
        httpServer.stop();
    }

    private void addResponseToRegistryServer(NationalIdentityNumber nin, CristinPersonResponse personCristinEntry) {
        stubFor(post("/person/identityNumber")
                    .withRequestBody(equalToJson(formatSearchByNinRequestBody(nin)))
                    .willReturn(aResponse().withBody(personCristinEntry.toString())));
    }

    private void cacheResponseForAssertions(NationalIdentityNumber nin, CristinPersonResponse personCristinEntry) {
        people.put(nin, personCristinEntry);
    }

    private CristinPersonResponse cristinPersonResponse(NationalIdentityNumber nin,
                                                        PersonEmployment... employmentAffiliations) {
        var affiliations = Arrays.stream(employmentAffiliations)
            .map(PersonEmployment::toCristinAffiliation)
            .collect(Collectors.toList());
        return CristinPersonResponse.builder()
            .withCristinId(randomPersonId())
            .withAffiliations(affiliations)
            .withNin(nin)
            .build();
    }

    private URI randomPersonId() {
        return UriWrapper.fromUri(serverUri).addChild("person").addChild(randomString()).getUri();
    }

    private String formatSearchByNinRequestBody(NationalIdentityNumber nin) {
        return String.format(PersonAndInstitutionRegistryClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
    }

    private void createResponseForOrganization(PersonEmployment orgStructure) {
        stubFor(WireMock.get(urlEqualTo(organizationPath(orgStructure.getChild())))
                    .willReturn(createInstitutionRegistryResponseForOrganization(orgStructure))

        );
    }

    private ResponseDefinitionBuilder createInstitutionRegistryResponseForOrganization(
        PersonEmployment employmentAffiliations) {
        return aResponse()
            .withStatus(HttpURLConnection.HTTP_OK)
            .withBody(organizationBody(employmentAffiliations));
    }

    private String organizationBody(PersonEmployment employmentAffiliation) {
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
