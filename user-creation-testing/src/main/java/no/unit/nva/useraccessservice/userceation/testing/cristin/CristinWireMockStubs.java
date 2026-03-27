package no.unit.nva.useraccessservice.userceation.testing.cristin;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitutionUnit;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static nva.commons.core.attempt.Try.attempt;

class CristinWireMockStubs {

    private static final String AUTHORIZATION_HEADER_NAME = "authorization";
    private static final String PERSONS_RESOLVE_BY_NATIONAL_ID = "/persons/resolve?national_id=%s";
    private static final String INSTITUTIONS = "/institutions/";
    private static final String PERSONS = "/persons";
    private final String basicAuthorizationHeaderValue;
    private final HttpHeaders defaultRequestHeaders;
    private URI cristinBaseUri;

    CristinWireMockStubs(String basicAuthorizationHeaderValue,
                         URI cristinBaseUri,
                         HttpHeaders defaultRequestHeaders) {
        this.basicAuthorizationHeaderValue = basicAuthorizationHeaderValue;
        this.cristinBaseUri = cristinBaseUri;
        this.defaultRequestHeaders = defaultRequestHeaders;
    }

    void setCristinBaseUri(URI cristinBaseUri) {
        this.cristinBaseUri = cristinBaseUri;
    }

    void createPersonSearchStubBadGateway(String nin) {
        stubWithDefaultRequestHeaders(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }

    void createPersonSearchStubIllegalJson(String nin) {
        var illegalBodyAsObjectIsExpected = "[]";

        stubWithDefaultRequestHeaders(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(illegalBodyAsObjectIsExpected)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    void createStubsForPerson(String nin, CristinPerson cristinPerson) {
        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson))
            .orElseThrow();

        stubWithDefaultRequestHeaders(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));

        stubWithDefaultRequestHeaders(
            get("/persons/" + cristinPerson.getId())
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    void createPostPersonStub(CristinPerson cristinPerson) {
        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson))
            .orElseThrow();

        stubWithDefaultRequestHeaders(
            post(PERSONS)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    void createStubForInstitution(String institutionIdentifier) {
        var institutionUnitIdentifier = institutionIdentifier + ".0.0.0";
        var urlForUnit = generateUnitUrl(institutionUnitIdentifier);
        var correspondingUnit = new CristinInstitutionUnit(institutionUnitIdentifier, urlForUnit);
        var cristinInstitution = new CristinInstitution(correspondingUnit);

        var response = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinInstitution))
            .orElseThrow();

        stubWithDefaultRequestHeaders(
            get(INSTITUTIONS + institutionIdentifier)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withBody(response)
                    .withStatus(HttpURLConnection.HTTP_OK)));
    }

    void createStubForRedirectingInstitution(String originalInstitutionId,
                                             String redirectedInstitutionId) {
        stubWithDefaultRequestHeaders(
            get(INSTITUTIONS + originalInstitutionId)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_MOVED_TEMP)
                    .withHeader("Location", INSTITUTIONS + redirectedInstitutionId)));
    }

    void createPersonNotFoundStub(String nin) {
        stubWithDefaultRequestHeaders(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    void createServerErrorStub(String nin) {
        stubWithDefaultRequestHeaders(
            get(PERSONS_RESOLVE_BY_NATIONAL_ID.formatted(nin))
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }

    void createPersonAlreadyExistsStub() {
        var errorBody = """
            {
                "status" : 400,
                "response_id" : "test123",
                 "errors" : [ "Norwegian national id already exists." ]
             }
            """;
        stubWithDefaultRequestHeaders(
            post(PERSONS)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_BAD_REQUEST)
                    .withBody(errorBody)));
    }

    void createPersonConflictStub() {
        var errorBody = """
            {
                "status" : 409,
                "response_id" : "test456",
                "errors" : [ "Conflict: Person already exists." ]
            }
            """;
        stubWithDefaultRequestHeaders(
            post(PERSONS)
                .withHeader(AUTHORIZATION_HEADER_NAME, equalTo(basicAuthorizationHeaderValue))
                .willReturn(aResponse()
                    .withStatus(HttpURLConnection.HTTP_CONFLICT)
                    .withBody(errorBody)));
    }

    String generateUnitUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("units", identifier).getUri().toString();
    }

    String generateInstitutionUrl(String identifier) {
        return UriWrapper.fromUri(cristinBaseUri).addChild("institutions", identifier).getUri().toString();
    }

    private void stubWithDefaultRequestHeaders(MappingBuilder mappingBuilder) {
        defaultRequestHeaders.stream()
            .forEach(entry -> mappingBuilder.withHeader(entry.getKey(), equalTo(entry.getValue())));

        stubFor(mappingBuilder);
    }
}
