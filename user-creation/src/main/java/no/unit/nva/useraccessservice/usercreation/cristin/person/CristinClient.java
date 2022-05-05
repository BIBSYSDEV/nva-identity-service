package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.useraccessservice.usercreation.CognitoUtils.COGNITO_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.CognitoUtils.COGNITO_HOST;
import static no.unit.nva.useraccessservice.usercreation.CognitoUtils.COGNITO_ID_KEY;
import static no.unit.nva.useraccessservice.usercreation.CognitoUtils.COGNITO_SECRET_KEY;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.cristin.org.CristinOrgResponse;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinClient {

    public static final String CRISTIN_PATH_FOR_GETTING_USER_BY_NIN = "person/identityNumber";
    public static final String REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE =
        "{\"type\":\"NationalIdentificationNumber\",\"value\":\"%s\"}";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String API_DOMAIN = ENVIRONMENT.readEnv("API_DOMAIN");
    public static final URI CRISTIN_HOST = UriWrapper.fromHost(API_DOMAIN).addChild("cristin").getUri();
    private static final String APPLICATION_JSON = "application/json";
    private static final Logger logger = LoggerFactory.getLogger(CristinClient.class);
    private final URI getUserByNinUri;
    private final AuthorizedBackendClient httpClient;

    public CristinClient(URI cristinHost, AuthorizedBackendClient httpClient) {
        this.httpClient = httpClient;
        this.getUserByNinUri = formatUriForGettingUserByNin(cristinHost);
    }

    @JacocoGenerated
    public static CristinClient defaultClient() {
        var secretsReader = new SecretsReader();
        var appClientId = secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_ID_KEY);
        var appClientSecret = secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_SECRET_KEY);
        var cognitoCredentials = new CognitoCredentials(appClientId, appClientSecret, COGNITO_HOST);
        var httpClient =
            AuthorizedBackendClient.prepareWithCognitoCredentials(cognitoCredentials);
        return new CristinClient(CRISTIN_HOST, httpClient);
    }

    public CristinPersonResponse sendRequestToCristin(NationalIdentityNumber nin)
        throws IOException, InterruptedException, BadGatewayException {
        var request = HttpRequest.newBuilder(getUserByNinUri)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .POST(BodyPublishers.ofString(cristinRequestBody(nin), StandardCharsets.UTF_8));
        var requestString = request.build().toString();
        logger.info("Request:{}", requestString);
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThatResponseIsSuccessful(response);
        return JsonConfig.readValue(response.body(), CristinPersonResponse.class);
    }

    public URI fetchTopLevelOrgUri(URI orgUri) throws IOException, InterruptedException, BadGatewayException {
        var request = HttpRequest.newBuilder(orgUri)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .GET();
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThatResponseIsSuccessful(response);

        var responseObject = CristinOrgResponse.fromJson(response.body());
        return responseObject.extractInstitutionUri();
    }

    private URI formatUriForGettingUserByNin(URI cristinHost) {
        return UriWrapper.fromUri(cristinHost)
            .addChild(CRISTIN_PATH_FOR_GETTING_USER_BY_NIN)
            .getUri();
    }

    private void assertThatResponseIsSuccessful(HttpResponse<String> response) throws BadGatewayException {
        if (response.statusCode() != HTTP_OK) {
            throw new BadGatewayException("Connection to Cristin failed." + response + " " + response.body());
        }
    }

    private String cristinRequestBody(NationalIdentityNumber nin) {
        return String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
    }
}
