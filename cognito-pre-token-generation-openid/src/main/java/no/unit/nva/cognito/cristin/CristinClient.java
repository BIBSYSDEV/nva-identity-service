package no.unit.nva.cognito.cristin;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_JSON;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import no.unit.nva.cognito.BadGatewayException;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.paths.UriWrapper;

public class CristinClient {

    public static final String CRISTIN_PATH_FOR_GETTING_USER_BY_NIN = "person/identityNumber";
    public static final String REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE =
        "{\"type\":\"NationalIdentificationNumber\",\"value\":\"%s\"}";
    private final URI getUserByNinUri;
    private final HttpClient httpClient;

    public CristinClient(URI cristinHost, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.getUserByNinUri = formatUriForGettingUserByNin(cristinHost);

    }

    public CristinResponse sendRequestToCristin(String jwtToken, String nin)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(getUserByNinUri)
            .setHeader(AUTHORIZATION_HEADER, "Bearer " + jwtToken)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .POST(BodyPublishers.ofString(cristinRequestBody(nin), StandardCharsets.UTF_8))
            .build();
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThatResponseIsSuccessful(response);
        return JsonConfig.objectMapper.beanFrom(CristinResponse.class,response.body());

    }

    private URI formatUriForGettingUserByNin(URI cristinHost) {
        return new UriWrapper(cristinHost)
            .addChild(CRISTIN_PATH_FOR_GETTING_USER_BY_NIN)
            .getUri();
    }

    private void assertThatResponseIsSuccessful(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new BadGatewayException("Connection to Cristin failed."+ response.toString());
        }
    }

    private String cristinRequestBody(String nin) {
        return String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin);
    }
}
