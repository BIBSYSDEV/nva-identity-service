package no.unit.nva.database;

import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TermsAndConditionsServiceTest {

    private static TermsAndConditionsService termsConditionsService;

    @BeforeAll
    static void initialize() {
        var client = DatabaseTestConfig
                .getEmbeddedClient();
        var TABLE_NAME = "PersistedObjectsTable";

        new SingleTableTemplateCreator(client)
                .createTable(TABLE_NAME);

        termsConditionsService = new TermsAndConditionsService(client, TABLE_NAME);

    }

    @Test
    void shouldUpdateTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri();
        var expectedResponse = TermsConditionsResponse.builder()
                .withTermsConditionsUri(randomUri())
                .build();

        var response = termsConditionsService
                .updateTermsAndConditions(
                        userIdentifier,
                        userIdentifier.toString(),
                        expectedResponse.termsConditionsUri()
                );

        var fetchedResponse = termsConditionsService
                .getTermsAndConditionsByPerson(userIdentifier);


        assertThat(expectedResponse, is(equalTo(response)));
        assertThat(expectedResponse, is(equalTo(fetchedResponse)));
    }

    @Test
    void shouldReturnCurrentTermsConditions() {
        var currentTermsAndConditions = termsConditionsService.getCurrentTermsAndConditions();
        assertThat(currentTermsAndConditions.termsConditionsUri(), is(equalTo(TermsAndConditionsService.TERMS_URL)));
    }

    @Test
    void shouldReturnTermsConditionsByPerson() throws NotFoundException {
        var userIdentifier = randomUri();
        var expectedResponse = TermsConditionsResponse.builder()
                .withTermsConditionsUri(randomUri())
                .build();

        termsConditionsService
                .updateTermsAndConditions(
                        userIdentifier,
                        userIdentifier.toString(),
                        expectedResponse.termsConditionsUri()
                );

        var fetchedResponse = termsConditionsService
                .getTermsAndConditionsByPerson(userIdentifier);

        assertThat(expectedResponse, is(equalTo(fetchedResponse)));
    }

    @Test
    void shouldReturnAllTermsConditions() {
        var allTermsAndConditions = termsConditionsService.getAllTermsAndConditions();
        assertThat(allTermsAndConditions.size(), is(equalTo(1)));
    }

    @Test
    void shouldReturnNullWhenTermsConditionsNotFound() {
        var userIdentifier = randomUri();
        var termsAndConditionsByPerson = termsConditionsService.getTermsAndConditionsByPerson(userIdentifier);
        assertNull(termsAndConditionsByPerson.termsConditionsUri());
    }

}