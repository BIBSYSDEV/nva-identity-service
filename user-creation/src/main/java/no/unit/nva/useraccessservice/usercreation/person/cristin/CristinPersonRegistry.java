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
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.PersonSearchResultItem;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static nva.commons.core.attempt.Try.attempt;

public class CristinPersonRegistry implements PersonRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CristinPersonRegistry.class);
    private static final String ERROR_MESSAGE_FORMAT = "Request %s %s failed with response code %d: %s";
    public static final String AUTHORIZATION = "Authorization";
    private final HttpClient httpClient;
    private final URI cristinBaseUri;
    private final Supplier<CristinCredentials> cristinCredentialsSupplier;

    public CristinPersonRegistry(URI cristinBaseUri, Supplier<CristinCredentials> credentialsSupplier) {
        this.httpClient = HttpClient.newHttpClient();
        this.cristinBaseUri = cristinBaseUri;
        this.cristinCredentialsSupplier = credentialsSupplier;
    }

    @Override
    public Optional<Person> fetchPersonByNin(String nin) {
        return fetchPersonByNinFromCristin(nin)
                   .map(this::fetchPersonFromCristin)
                   .map(this::asPerson);
    }

    private CristinPerson fetchPersonFromCristin(PersonSearchResultItem personSearchResultItem) {
        var request = createRequest(personSearchResultItem.getUrl());
        return doRequestReturningString(request, CristinPerson.class);
    }

    private CristinInstitution fetchInstitutionFromCristin(URI institutionUri) {
        var request = createRequest(institutionUri);
        return doRequestReturningString(request, CristinInstitution.class);
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .GET()
                .header(AUTHORIZATION, generateBasicAuthorization())
                .build();
    }

    private static <T> T fromJson(String responseAsString, Class<T> type) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(responseAsString, type))
                .orElseThrow(failure -> new PersonRegistryException("Failed to parse response!", failure.getException()));
    }

    private Person asPerson(CristinPerson cristinPerson) {

        // This may not actually work, but you see the point?
        var personAffiliations = cristinPerson.getAffiliations().stream()
                .filter(CristinAffiliation::isActive)
                .map(this::collectAffiliation)
                .collect(Collectors.groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())))
                .entrySet().stream()
                .map(a -> new Affiliation(a.getKey(), a.getValue()))
                .collect(Collectors.toList());

        return new Person(cristinPerson.getId(), cristinPerson.getFirstname(), cristinPerson.getSurname(),
                          personAffiliations);
    }

    private Pair collectAffiliation(CristinAffiliation cristinAffiliation) {
        var institutionUri = cristinAffiliation.getInstitution().getUri();
        var cristinInstitution = fetchInstitutionFromCristin(institutionUri);
        var institutionId = cristinInstitution.getCorrespondingUnit().getId();
        return new Pair(institutionId, cristinAffiliation.getUnit().getId());
    }

    private Optional<PersonSearchResultItem> fetchPersonByNinFromCristin(String nin) {

        var responseAsString = doRequestReturningString(createPersonByNationalIdentityNumberQueryRequest(nin), PersonSearchResultItem[].class);

        return Arrays.stream(responseAsString).findFirst();
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

    private <T> T doRequestReturningString(HttpRequest request, Class<T> type) {
        final HttpResponse<String> response;
        var start = Instant.now();
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PersonRegistryException("Cristin is unavailable", e);
        } finally {
            LOGGER.info("Called {} and got response in {} ms.", request.uri(),
                        Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        assertOkResponse(request, response);

        return fromJson(response.body(), type);
    }

    private void assertOkResponse(HttpRequest request, HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            var message = generateErrorMessageForResponse(request, response);
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

    private static class Pair {
        private final String left;
        private final String right;

        public Pair(String left, String right) {
            this.left = left;
            this.right = right;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }
    }
}
