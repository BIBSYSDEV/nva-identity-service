package no.unit.nva.useraccessservice.usercreation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinPersonResponse;
import nva.commons.core.paths.UriWrapper;

public class CristinServerMock {

    private URI serverUri;

    public CristinServerMock() {
        setUpWiremock();
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void addPerson(NationalIdentityNumber nin, PersonEmployment... employmentAffiliations) {
        for (var employmentAffiliation : employmentAffiliations) {
            createResponseForOrganization(employmentAffiliation);
        }
        stubFor(post("/person/" + nin.getNin())
                    .withRequestBody(equalToJson(formatSearchByNinRequestBody(nin)))
                    .willReturn(aResponse().withBody(cristinPersonResponse(nin,employmentAffiliations))));
    }

    private String cristinPersonResponse(NationalIdentityNumber nin,PersonEmployment... employmentAffiliations) {
        var affiliations = Arrays.stream(employmentAffiliations)
            .map(PersonEmployment::toCristinAffiliation)
            .collect(Collectors.toList());
        return CristinPersonResponse.builder()
            .withCristinId(randomUri())
            .withAffiliations(affiliations)
            .withNin(nin)
            .build()
            .toString();
    }

    private String formatSearchByNinRequestBody(NationalIdentityNumber nin) {
        return String.format(CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
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
        parent.setOrgId(employmentAffiliation.getChild());
        var response = new CristinOrgResponse();
        response.setOrgId(employmentAffiliation.getChild());
        response.setPartOf(List.of(parent));
        return response.toString();
    }

    private String organizationPath(URI organization) {
        return UriWrapper.fromUri(organization).getPath().toString();
    }

    private void setUpWiremock() {
        WireMockServer httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }
}
