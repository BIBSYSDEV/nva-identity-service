package no.unit.nva.useraccessservice.usercreation.person.cristin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.PersonSearchResultItem;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_NAME;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static nva.commons.core.attempt.Try.attempt;

public final class CristinPersonRegistry implements PersonRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CristinPersonRegistry.class);
    private static final String ERROR_MESSAGE_FORMAT = "Request %s %s failed with response code %d: %s";
    private static final String HTTPS_SCHEME = "https";
    private static final String CRISTIN_PATH = "cristin";
    private static final String PERSON_PATH = "person";
    private static final String ORGANIZATION_PATH = "organization";
    private static final String NATIONAL_IDENTITY_PATTERN = ".*\\?national_id=(\\d+)\\d\\d$";
    public static final String AUTHORIZATION = "Authorization";

    public static final String CRISTIN_CREDENTIALS_SECRET_NAME = "CristinClientBasicAuth";
    public static final String CRISTIN_USERNAME_SECRET_KEY = "username";
    public static final String CRISTIN_PASSWORD_SECRET_KEY = "password";
    private final HttpClient httpClient;
    private final URI cristinBaseUri;
    private final String apiDomain;
    private final HttpHeaders defaultRequestHeaders;
    private final Supplier<CristinCredentials> cristinCredentialsSupplier;

    private CristinPersonRegistry(HttpClient httpClient,
                                  URI cristinBaseUri,
                                  String apiDomain,
                                  HttpHeaders defaultRequestHeaders,
                                  Supplier<CristinCredentials> credentialsSupplier) {
        this.httpClient = httpClient;
        this.cristinBaseUri = cristinBaseUri;
        this.apiDomain = apiDomain;
        this.defaultRequestHeaders = defaultRequestHeaders;
        this.cristinCredentialsSupplier = credentialsSupplier;
    }

    @JacocoGenerated
    public static PersonRegistry defaultPersonRegistry() {
        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
        return personRegistry(HttpClient.newBuilder()
                                  .version(Version.HTTP_1_1)
                                  .followRedirects(Redirect.NORMAL)
                                  .build(),
                              ServiceConstants.CRISTIN_BASE_URI,
                              ServiceConstants.API_DOMAIN,
                              defaultRequestHeaders,
                              new SecretsReader());
    }

    public static PersonRegistry customPersonRegistry(HttpClient httpClient,
                                                      URI cristinBaseUri,
                                                      String apiDomain,
                                                      HttpHeaders defaultRequestHeaders,
                                                      SecretsReader secretsReader) {
        return personRegistry(httpClient,
                              cristinBaseUri,
                              apiDomain,
                              defaultRequestHeaders,
                              secretsReader);
    }

    private static PersonRegistry personRegistry(HttpClient httpClient,
                                                 URI cristinBaseUri,
                                                 String apiDomain,
                                                 HttpHeaders defaultRequestHeaders,
                                                 SecretsReader secretsReader) {
        return new CristinPersonRegistry(httpClient,
                                         cristinBaseUri,
                                         apiDomain,
                                         defaultRequestHeaders,
                                         secretsReaderCristinCredentialsSupplier(secretsReader));
    }

    private static Supplier<CristinCredentials> secretsReaderCristinCredentialsSupplier(SecretsReader secretsReader) {
        return () -> secretsReader.fetchClassSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CristinCredentials.class);
    }

    @Override
    public Optional<Person> fetchPersonByNin(NationalIdentityNumber nin) {
        var start = Instant.now();
        var cristinCredentials = this.cristinCredentialsSupplier.get();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Read cristin credentials from secrets manager in {} ms.",
                         Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        return fetchPersonByNinFromCristin(nin, cristinCredentials)
                   .map(person -> fetchPersonFromCristin(person, cristinCredentials))
                   .map(cristinPerson -> asPerson(cristinPerson, cristinCredentials));
    }

    private CristinPerson fetchPersonFromCristin(PersonSearchResultItem personSearchResultItem,
                                                 CristinCredentials cristinCredentials) {
        var request = createRequest(URI.create(personSearchResultItem.getUrl()), cristinCredentials);
        return executeRequest(request, CristinPerson.class);
    }

    private CristinInstitution fetchInstitutionFromCristin(URI institutionUri, CristinCredentials cristinCredentials) {
        var request = createRequest(institutionUri, cristinCredentials);
        return executeRequest(request, CristinInstitution.class);
    }

    private HttpRequest createRequest(URI uri, CristinCredentials cristinCredentials) {
        var requestBuilder = HttpRequest.newBuilder(uri)
                   .GET()
                   .header(AUTHORIZATION, generateBasicAuthorization(cristinCredentials));

        defaultRequestHeaders.stream()
            .forEach(entry -> requestBuilder.header(entry.getKey(), entry.getValue()));

        return requestBuilder.build();
    }

    private static <T> T fromJson(String responseAsString, Class<T> type) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(responseAsString, type))
                   .orElseThrow(CristinPersonRegistry::logAndThrowException);
    }

    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    private static <T> PersonRegistryException logAndThrowException(Failure<T> failure) {
        LOGGER.error("Got unexpected response body from Cristin", failure.getException());
        return new PersonRegistryException("Failed to parse response!", failure.getException());
    }

    private Person asPerson(CristinPerson cristinPerson, CristinCredentials cristinCredentials) {

        var personAffiliations
            = cristinPerson.getAffiliations().stream()
                  .filter(CristinAffiliation::isActive)
                  .map(activeAffiliation -> collectAffiliation(activeAffiliation, cristinCredentials))
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

    private GenericPair<URI> collectAffiliation(CristinAffiliation cristinAffiliation,
                                                CristinCredentials cristinCredentials) {

        var institutionUri = URI.create(cristinAffiliation.getInstitution().getUrl());
        var cristinInstitution = fetchInstitutionFromCristin(institutionUri, cristinCredentials);
        var institutionId = cristinInstitution.getCorrespondingUnit().getId();
        return new GenericPair<>(generateCristinIdForOrganization(institutionId),
                                 generateCristinIdForOrganization(cristinAffiliation.getUnit().getId()));
    }

    private Optional<PersonSearchResultItem> fetchPersonByNinFromCristin(NationalIdentityNumber nin,
                                                                         CristinCredentials cristinCredentials) {

        var request = createRequest(createPersonByNationalIdentityNumberQueryUri(nin), cristinCredentials);
        var results = executeRequest(request, PersonSearchResultItem[].class);

        return Arrays.stream(results).collect(SingletonCollector.tryCollect()).toOptional();
    }

    private URI createPersonByNationalIdentityNumberQueryUri(NationalIdentityNumber nin) {
        return UriWrapper.fromUri(cristinBaseUri)
                   .addChild("persons")
                   .addQueryParameter("national_id", nin.getNin())
                   .getUri();
    }

    private <T> T executeRequest(HttpRequest request, Class<T> type) {
        final HttpResponse<String> response;
        var start = Instant.now();
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Cristin is unavailable", e);
            conditionallyInterrupt(e);
            throw new PersonRegistryException("Cristin is unavailable", e);
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Called {} and got response in {} ms.",
                             maskSensitiveData(request.uri()),
                             Instant.now().toEpochMilli() - start.toEpochMilli());
            }
        }

        assertOkResponse(request, response);

        return fromJson(response.body(), type);
    }

    @JacocoGenerated
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

    private String generateBasicAuthorization(CristinCredentials cristinCredentials) {
        var toBeEncoded =
            (cristinCredentials.getUsername() + ":" + new String(cristinCredentials.getPassword())).getBytes();
        return "Basic " + Base64.getEncoder().encodeToString(toBeEncoded);
    }

    private String maskSensitiveData(URI uri) {
        var pattern = Pattern.compile(NATIONAL_IDENTITY_PATTERN);
        var matcher = pattern.matcher(uri.toString());
        String maskedUri;
        if (matcher.matches()) {
            var toMask = matcher.group(1);
            maskedUri = uri.toString().replaceAll(toMask, "XXXXXXXXX");
        } else {
            maskedUri = uri.toString();
        }

        return maskedUri;
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
