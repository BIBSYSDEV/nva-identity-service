package no.unit.nva.useraccessservice.usercreation.person.cristin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.PersonSearchResultItem;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static nva.commons.core.attempt.Try.attempt;

public class CristinPersonRegistry implements PersonRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CristinPersonRegistry.class);
    private static final String ERROR_MESSAGE_FORMAT = "Request %s %s failed with response code %d: %s";
    private static final String HTTPS_SCHEME = "https";
    private static final String CRISTIN_PATH = "cristin";
    private static final String PERSON_PATH = "person";
    private static final String ORGANIZATION_PATH = "organization";
    public static final String AUTHORIZATION = "Authorization";

    public static final String CRISTIN_CREDENTIALS_SECRET_NAME = "CristinClientBasicAuth";
    public static final String CRISTIN_USERNAME_SECRET_KEY = "username";
    public static final String CRISTIN_PASSWORD_SECRET_KEY = "password";
    private final HttpClient httpClient;
    private final URI cristinBaseUri;
    private final String apiDomain;
    private final Supplier<CristinCredentials> cristinCredentialsSupplier;

    private CristinPersonRegistry(HttpClient httpClient,
                                  URI cristinBaseUri,
                                  String apiDomain,
                                  Supplier<CristinCredentials> credentialsSupplier) {
        this.httpClient = httpClient;
        this.cristinBaseUri = cristinBaseUri;
        this.apiDomain = apiDomain;
        this.cristinCredentialsSupplier = credentialsSupplier;
    }

    @JacocoGenerated
    public static PersonRegistry defaultPersonRegistry() {
        return personRegistry(HttpClient.newHttpClient(), ServiceConstants.CRISTIN_BASE_URI,
                              ServiceConstants.API_DOMAIN, new SecretsReader());
    }

    public static PersonRegistry customPersonRegistry(HttpClient httpClient,
                                                      URI cristinBaseUri,
                                                      String apiDomain,
                                                      SecretsReader secretsReader) {
        return personRegistry(httpClient, cristinBaseUri, apiDomain, secretsReader);
    }

    private static PersonRegistry personRegistry(HttpClient httpClient,
                                                 URI cristinBaseUri,
                                                 String apiDomain,
                                                 SecretsReader secretsReader) {
        return new CristinPersonRegistry(httpClient, cristinBaseUri, apiDomain,
                                         secretsReaderCristinCredentialsSupplier(secretsReader));
    }

    private static Supplier<CristinCredentials> secretsReaderCristinCredentialsSupplier(SecretsReader secretsReader) {
        return () -> new CristinCredentials(
            secretsReader.fetchSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY),
            secretsReader.fetchSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY));
    }

    @Override
    public Optional<Person> fetchPersonByNin(String nin) {
        return fetchPersonByNinFromCristin(nin)
                   .map(this::fetchPersonFromCristin)
                   .map(this::asPerson);
    }

    private CristinPerson fetchPersonFromCristin(PersonSearchResultItem personSearchResultItem) {
        var request = createRequest(personSearchResultItem.getUrl());
        return executeRequest(request, CristinPerson.class);
    }

    private CristinInstitution fetchInstitutionFromCristin(URI institutionUri) {
        var request = createRequest(institutionUri);
        return executeRequest(request, CristinInstitution.class);
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                   .GET()
                   .header(AUTHORIZATION, generateBasicAuthorization())
                   .build();
    }

    private static <T> T fromJson(String responseAsString, Class<T> type) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(responseAsString, type))
                   .orElseThrow(
                       failure -> new PersonRegistryException("Failed to parse response!", failure.getException()));
    }

    private Person asPerson(CristinPerson cristinPerson) {

        var personAffiliations
            = cristinPerson.getAffiliations().stream()
                  .filter(CristinAffiliation::isActive)
                  .map(this::collectAffiliation)
                  .collect(Collectors.groupingBy(GenericPair::getLeft, mapping(GenericPair::getRight, toList())))
                  .entrySet().stream()
                  .map(a -> new Affiliation(a.getKey(), a.getValue()))
                  .collect(toList());

        return new Person(generateCristinIdForPerson(cristinPerson.getId()),
                          cristinPerson.getId(),
                          cristinPerson.getFirstname(),
                          cristinPerson.getSurname(),
                          personAffiliations);
    }

    private URI generateCristinIdForOrganization(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, apiDomain)
                   .addChild(CRISTIN_PATH, ORGANIZATION_PATH, identifier)
                   .getUri();
    }

    private URI generateCristinIdForPerson(String identifier) {
        return new UriWrapper(HTTPS_SCHEME, apiDomain)
                   .addChild(CRISTIN_PATH, PERSON_PATH, identifier)
                   .getUri();
    }

    private GenericPair<URI> collectAffiliation(CristinAffiliation cristinAffiliation) {
        var institutionUri = cristinAffiliation.getInstitution().getUri();
        var cristinInstitution = fetchInstitutionFromCristin(institutionUri);
        var institutionId = cristinInstitution.getCorrespondingUnit().getId();
        return new GenericPair<>(generateCristinIdForOrganization(institutionId),
                                 generateCristinIdForOrganization(cristinAffiliation.getUnit().getId()));
    }

    private Optional<PersonSearchResultItem> fetchPersonByNinFromCristin(String nin) {

        var results = executeRequest(createPersonByNationalIdentityNumberQueryRequest(nin),
                                     PersonSearchResultItem[].class);

        return Arrays.stream(results).collect(SingletonCollector.tryCollect()).toOptional();
    }

    private HttpRequest createPersonByNationalIdentityNumberQueryRequest(String nin) {
        return HttpRequest.newBuilder(createPersonByNationalIdentityNumberQueryUri(nin))
                   .GET()
                   .header(AUTHORIZATION, generateBasicAuthorization())
                   .build();
    }

    private URI createPersonByNationalIdentityNumberQueryUri(String nin) {
        return UriWrapper.fromUri(cristinBaseUri)
                   .addChild("persons")
                   .addQueryParameter("national_id", nin)
                   .getUri();
    }

    private <T> T executeRequest(HttpRequest request, Class<T> type) {
        final HttpResponse<String> response;
        var start = Instant.now();
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            conditionallyInterrupt(e);
            throw new PersonRegistryException("Cristin is unavailable", e);
        } finally {
            LOGGER.info("Called {} and got response in {} ms.", request.uri(),
                        Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        assertOkResponse(request, response);

        return fromJson(response.body(), type);
    }

    private static void conditionallyInterrupt(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void assertOkResponse(HttpRequest request, HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            var message = generateErrorMessageForResponse(request, response);
            LOGGER.info(message);
            throw new PersonRegistryException(message);
        }
    }

    private String generateErrorMessageForResponse(HttpRequest request, HttpResponse<String> response) {
        return String.format(ERROR_MESSAGE_FORMAT,
                             request.method(),
                             request.uri(),
                             response.statusCode(),
                             response.body());
    }

    private String generateBasicAuthorization() {
        var cristinCredentials = this.cristinCredentialsSupplier.get();
        var toBeEncoded = (cristinCredentials.getUsername() + ":" + cristinCredentials.getPassword()).getBytes();
        return "Basic " + Base64.getEncoder().encodeToString(toBeEncoded);
    }

    private static class GenericPair<T> {

        private final T left;
        private final T right;

        public GenericPair(T left, T right) {
            this.left = left;
            this.right = right;
        }

        public T getLeft() {
            return left;
        }

        public T getRight() {
            return right;
        }
    }
}
