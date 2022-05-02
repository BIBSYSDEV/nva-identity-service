package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import nva.commons.apigatewayv2.exceptions.BadGatewayException;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinClient {

    public static final String CRISTIN_PATH_FOR_GETTING_USER_BY_NIN = "person/identityNumber";
    public static final String REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE =
        "{\"type\":\"NationalIdentificationNumber\",\"value\":\"%s\"}";
    private static final String APPLICATION_JSON = "application/json";
    private final URI getUserByNinUri;
    private final AuthorizedBackendClient httpClient;
    private static final Logger logger = LoggerFactory.getLogger(CristinClient.class);

    public CristinClient(URI cristinHost, AuthorizedBackendClient httpClient) {
        this.httpClient = httpClient;
        this.getUserByNinUri = formatUriForGettingUserByNin(cristinHost);
    }

    public CristinPersonResponse sendRequestToCristin(NationalIdentityNumber nin)
        throws IOException, InterruptedException {
        var requestBody = cristinRequestBody(nin);
        var request = HttpRequest.newBuilder(getUserByNinUri)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .POST(BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
        logger.info(requestBody);
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThatResponseIsSuccessful(response);
        return JsonConfig.readValue(response.body(), CristinPersonResponse.class);
    }

    public URI fetchTopLevelOrgUri(URI orgUri) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(orgUri)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .GET();
        var response = httpClient.send(request,BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThatResponseIsSuccessful(response);

        var responseObject = CristinOrgResponse.fromJson(response.body());
        return responseObject.extractTopOrgUri();

    }

    private URI formatUriForGettingUserByNin(URI cristinHost) {
        return UriWrapper.fromUri(cristinHost)
            .addChild(CRISTIN_PATH_FOR_GETTING_USER_BY_NIN)
            .getUri();
    }

    private void assertThatResponseIsSuccessful(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new BadGatewayException("Connection to Cristin failed." + response);
        }
    }

    private String cristinRequestBody(NationalIdentityNumber nin) {
        return String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
    }
}
