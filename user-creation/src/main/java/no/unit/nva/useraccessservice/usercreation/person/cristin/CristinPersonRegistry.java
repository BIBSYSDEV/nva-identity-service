package no.unit.nva.useraccessservice.usercreation.person.cristin;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinInstitution;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.CristinPerson;
import no.unit.nva.useraccessservice.usercreation.person.cristin.model.PersonSearchResultItem;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinPersonRegistry implements PersonRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CristinPersonRegistry.class);
    private static final String ERROR_MESSAGE_FORMAT = "Request %s %s failed with response code %d: %s";
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
        var request = HttpRequest.newBuilder(URI.create(personSearchResultItem.getUrl()))
                          .GET()
                          .header("Authorization", generateBasicAuthorization())
                          .build();

        var responseAsString = doRequestReturningString(request);

        try {
            return JsonUtils.dtoObjectMapper.readValue(responseAsString, CristinPerson.class);
        } catch (JsonProcessingException e) {
            throw new PersonAuthorityException("Failed to parse response!", e);
        }
    }

    private CristinInstitution fetchInstitutionFromCristin(URI institutionUri) {
        var request = HttpRequest.newBuilder(institutionUri)
                          .GET()
                          .header("Authorization", generateBasicAuthorization())
                          .build();

        var responseAsString = doRequestReturningString(request);

        try {
            return JsonUtils.dtoObjectMapper.readValue(responseAsString, CristinInstitution.class);
        } catch (JsonProcessingException e) {
            throw new PersonAuthorityException("Failed to parse response!", e);
        }
    }

    private Person asPerson(CristinPerson cristinPerson) {
        Map<String, List<String>> affiliations = new ConcurrentHashMap<>();
        cristinPerson.getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .forEach(cristinAffiliation -> collectAffiliation(affiliations, cristinAffiliation));

        List<Affiliation> personAffiliations = new ArrayList<>();
        affiliations.forEach((key, value) -> personAffiliations.add(new Affiliation(key, value)));

        return new Person(cristinPerson.getId(), cristinPerson.getFirstname(), cristinPerson.getSurname(),
                          personAffiliations);
    }

    private void collectAffiliation(Map<String, List<String>> affiliations,
                                    CristinAffiliation cristinAffiliation) {
        URI institutionUri = URI.create(cristinAffiliation.getInstitution().getUrl());
        var cristinInstitution = fetchInstitutionFromCristin(institutionUri);

        var institutionId = cristinInstitution.getCorrespondingUnit().getId();
        affiliations.computeIfAbsent(institutionId, s -> new ArrayList<>()).add(cristinAffiliation.getUnit().getId());
    }

    private Optional<PersonSearchResultItem> fetchPersonByNinFromCristin(String nin) {
        var requestUri = UriWrapper.fromUri(cristinBaseUri)
                             .addChild("persons")
                             .addQueryParameter("national_id", nin)
                             .getUri();

        var request = HttpRequest.newBuilder(requestUri)
                          .GET()
                          .header("Authorization", generateBasicAuthorization())
                          .build();

        var responseAsString = doRequestReturningString(request);

        try {
            var resultItems = JsonUtils.dtoObjectMapper.readValue(responseAsString, PersonSearchResultItem[].class);
            return Arrays.stream(resultItems).findFirst();
        } catch (JsonProcessingException e) {
            throw new PersonAuthorityException("Failed to parse response!", e);
        }
    }

    private String doRequestReturningString(HttpRequest request) {
        final HttpResponse<String> response;
        var start = Instant.now();
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            throw new PersonAuthorityException("Cristin is unavailable", e);
        } finally {
            LOGGER.info("Called {} and got response in {} ms.", request.uri(),
                        Instant.now().toEpochMilli() - start.toEpochMilli());
        }

        assertOkResponse(request, response);

        return response.body();
    }

    private void assertOkResponse(HttpRequest request, HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            var message = generateErrorMessageForResponse(request, response);
            throw new PersonAuthorityException(message);
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
}
