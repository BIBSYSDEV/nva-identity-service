package no.unit.nva.database;

import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.database.TermsAndConditionsService.TERMS_TABLE_NAME_ENV;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TermsAndConditionsServiceTest {

    private static TermsAndConditionsService termsConditionsService;

    @BeforeAll
    static void initialize() {
        var client = DatabaseTestConfig
                .getEmbeddedClient();
        var termsTable = "TermsTable";

        new SingleTableTemplateCreator(client)
                .createTable(termsTable);

        var environment = mock(Environment.class);
        when(environment.readEnv(TERMS_TABLE_NAME_ENV)).thenReturn(termsTable);

        termsConditionsService = new TermsAndConditionsService(client, environment);

    }

    @Test
    void shouldUpdateTermsConditions() throws NotFoundException {
        var cristinPersonId = randomUri();
        var userId = randomString();
        var expectedResponse = TermsConditionsResponse.builder()
                .withTermsConditionsUri(randomUri())
                .build();

        var response = termsConditionsService
                .updateTermsAndConditions(
                        cristinPersonId,
                        expectedResponse.termsConditionsUri(),
                        userId
                );

        var fetchedResponse = termsConditionsService
                .getTermsAndConditionsByPerson(cristinPersonId);


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
        var cristinPersonId = randomUri();
        var userId = randomString();

        var expectedResponse = TermsConditionsResponse.builder()
                .withTermsConditionsUri(randomUri())
                .build();

        termsConditionsService
                .updateTermsAndConditions(
                    cristinPersonId,
                        expectedResponse.termsConditionsUri(),
                        userId
                );

        var fetchedResponse = termsConditionsService
                .getTermsAndConditionsByPerson(cristinPersonId);

        assertThat(expectedResponse, is(equalTo(fetchedResponse)));
    }

    @Test
    void shouldReturnAllTermsConditions() {
        var allTermsAndConditions = termsConditionsService.getAllTermsAndConditions();
        assertThat(allTermsAndConditions.size(), is(equalTo(1)));
    }

    @Test
    void shouldReturnNullWhenTermsConditionsNotFound() {
        var cristinPersonId = randomUri();
        var termsAndConditionsByPerson = termsConditionsService.getTermsAndConditionsByPerson(cristinPersonId);
        assertNull(termsAndConditionsByPerson);
    }

}