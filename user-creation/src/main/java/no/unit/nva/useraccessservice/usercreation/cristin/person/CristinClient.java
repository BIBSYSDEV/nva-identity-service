package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.CRISTIN_HOST;
import static no.unit.nva.useraccessservice.usercreation.CognitoConstants.COGNITO_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.CognitoConstants.COGNITO_HOST;
import static no.unit.nva.useraccessservice.usercreation.CognitoConstants.COGNITO_ID_KEY;
import static no.unit.nva.useraccessservice.usercreation.CognitoConstants.COGNITO_SECRET_KEY;
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
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinClient {
    
    public static final String CRISTIN_PATH_FOR_GETTING_USER_BY_NIN = "person/identityNumber";
    public static final String REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE =
        "{\"type\":\"NationalIdentificationNumber\",\"value\":\"%s\"}";
    
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
        return defaultClient(new SecretsReader());
    }
    
    public URI fetchTopLevelOrgUri(URI orgUri) throws IOException, InterruptedException, BadGatewayException {
        var request = HttpRequest.newBuilder(orgUri)
            .setHeader(CONTENT_TYPE, APPLICATION_JSON)
            .GET();
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        
        assertThatResponseIsSuccessful(orgUri, response);
        
        var responseObject = CristinOrgResponse.fromJson(response.body());
        return responseObject.extractInstitutionUri();
    }
    
    protected static CristinClient defaultClient(SecretsReader secretsReader) {
        var cognitoCredentials = new CognitoCredentials(
            () -> fetchLatestValueOfAppClientIdForHotInstance(secretsReader),
            () -> fetchLatestValueOfAppClientSecretForHotInstance(secretsReader),
            COGNITO_HOST);
        var httpClient =
            AuthorizedBackendClient.prepareWithCognitoCredentials(cognitoCredentials);
        return new CristinClient(CRISTIN_HOST, httpClient);
    }
    
    @JacocoGenerated
    private static String fetchLatestValueOfAppClientSecretForHotInstance(SecretsReader secretsReader) {
        return secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_SECRET_KEY);
    }
    
    public CristinPersonResponse fetchPersonInformation(NationalIdentityNumber nin)
        throws IOException, InterruptedException, BadGatewayException {
        var request = HttpRequest.newBuilder(getUserByNinUri)
                          .setHeader(CONTENT_TYPE, APPLICATION_JSON)
                          .POST(BodyPublishers.ofString(cristinRequestBody(nin), StandardCharsets.UTF_8));
        var requestString = request.build().toString();
        logger.info("Request:{}", requestString);
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThatResponseIsSuccessful(nin, response);
        return JsonConfig.readValue(response.body(), CristinPersonResponse.class);
    }
    
    @JacocoGenerated
    private static String fetchLatestValueOfAppClientIdForHotInstance(SecretsReader secretsReader) {
        return secretsReader.fetchSecret(COGNITO_CREDENTIALS_SECRET_NAME, COGNITO_ID_KEY);
    }
    
    private URI formatUriForGettingUserByNin(URI cristinHost) {
        return UriWrapper.fromUri(cristinHost)
            .addChild(CRISTIN_PATH_FOR_GETTING_USER_BY_NIN)
            .getUri();
    }
    
    private <T> void assertThatResponseIsSuccessful(T entityIdentifier,
                                                    HttpResponse<String> response) throws BadGatewayException {
        if (response.statusCode() != HTTP_OK) {
            var message = createWarningForFailedRequestToPersonRegistry(entityIdentifier, response);
            logger.warn(message);
            throw new BadGatewayException(message);
        }
    }
    
    private <T> String createWarningForFailedRequestToPersonRegistry(T entityIdentifier,
                                                                     HttpResponse<String> response) {
        return String.format("Connection to Cristin failed for %s. Response %s",
            entityIdentifier.toString(),
            response.body());
    }
    
    private String cristinRequestBody(NationalIdentityNumber nin) {
        return String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.getNin());
    }
}
