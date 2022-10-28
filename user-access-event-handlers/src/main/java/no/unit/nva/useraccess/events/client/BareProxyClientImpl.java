package no.unit.nva.useraccess.events.client;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.API_DOMAIN;
import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.services.eventbridge.model.ApiDestinationHttpMethod.DELETE;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BareProxyClientImpl implements BareProxyClient {

    public static final String ERROR_READING_SECRETS_ERROR =
            "Could not read secrets for internal communication with Bare Proxy";
    public static final String RESPONSE_STATUS_BODY = "Response status=%s, body=%s";
    public static final String CREATING_REQUEST_TO = "Creating request to: {}";
    public static final String HTTPS_SCHEME = "https";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String BARE_PROXY_SECRET_NAME = ENVIRONMENT
            .readEnv("BARE_PROXY_SECRET_NAME");
    public static final String BARE_PROXY_SECRET_KEY = ENVIRONMENT
            .readEnv("BARE_PROXY_SECRET_KEY");
    
    public static final String PERSON_INTERNAL_SERVICE_PATH = "person-internal";
    public static final String PERSON_SERVICE_PATH = "person";
    public static final String FEIDE_ID_QUERY_PARAM = "feideid";
    public static final String DELETE_ORG_ID_ERROR = "Error deleting organizationId from authority";
    public static final String GET_AUTHORIY_ERROR = "Error getting authority for feideId";

    private final Logger logger = LoggerFactory.getLogger(BareProxyClientImpl.class);
    private final HttpClient httpClient;
    private final String bareProxySecret;

    public BareProxyClientImpl(SecretsReader secretsReader, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.bareProxySecret = attempt(() -> fetchSecret(secretsReader))
                .orElseThrow(fail -> logAndFail(fail.getException()));
    }

    @JacocoGenerated
    public BareProxyClientImpl() {
        this(new SecretsReader(), HttpClient.newHttpClient());
    }

    @Override
    public Optional<SimpleAuthorityResponse> getAuthorityByFeideId(String feideId) {
        Optional<SimpleAuthorityResponse> authorityResponse = Optional.empty();
        try {
            HttpRequest request = createAuthorityGetHttpRequest(feideId);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                List<SimpleAuthorityResponse> list = fromJson(response);
                authorityResponse = list.stream().findAny();
            } else {
                logWarning(response);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_AUTHORIY_ERROR, e);
        }
        return authorityResponse;
    }

    private List<SimpleAuthorityResponse> fromJson(HttpResponse<String> response) throws IOException {
        return JsonConfig.listOfFrom(SimpleAuthorityResponse.class, response.body());
    }

    @Override
    public void deleteAuthorityOrganizationId(String systemControlNumber, URI organizationId) {
        try {
            HttpRequest request = createAuthorityDeleteHttpRequest(systemControlNumber, organizationId);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                //TODO: how do we want to react if Bare update fails?
                logWarning(response);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(DELETE_ORG_ID_ERROR, e);
        }
    }

    private void logWarning(HttpResponse<String> response) {
        logger.warn(String.format(RESPONSE_STATUS_BODY, response.statusCode(), response.body()));
    }

    @JacocoGenerated
    private RuntimeException logAndFail(Exception exception) {
        logger.error(ERROR_READING_SECRETS_ERROR);
        return new RuntimeException(exception);
    }

    private String fetchSecret(SecretsReader secretsReader) {
        return secretsReader.fetchSecret(BARE_PROXY_SECRET_NAME, BARE_PROXY_SECRET_KEY);
    }

    private HttpRequest createAuthorityDeleteHttpRequest(String systemControlNumber, URI organizationId) {
        URI uri = new UriWrapper(HTTPS_SCHEME, API_DOMAIN)
                      .addChild(PERSON_INTERNAL_SERVICE_PATH)
                      .addChild(systemControlNumber)
                      .addChild("identifiers")
                      .addChild("orgunitid")
                      .addChild("delete")
                      .getUri();
        logger.info(CREATING_REQUEST_TO, uri);
        DeleteIdentifierRequest body = new DeleteIdentifierRequest(organizationId);
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .headers(HttpHeaders.ACCEPT, JSON_UTF_8.toString(), AUTHORIZATION, bareProxySecret)
                   .method(DELETE.name(), HttpRequest.BodyPublishers.ofString(body.toJson()))
                   .build();
    }

    private HttpRequest createAuthorityGetHttpRequest(String feideId) {
        URI uri = new UriWrapper(HTTPS_SCHEME, API_DOMAIN)
                      .addChild(PERSON_SERVICE_PATH)
                      .addQueryParameter(FEIDE_ID_QUERY_PARAM, feideId)
                      .getUri();
        logger.info(CREATING_REQUEST_TO, uri);
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .headers(HttpHeaders.ACCEPT, JSON_UTF_8.toString())
                   .GET()
                   .build();
    }
}
