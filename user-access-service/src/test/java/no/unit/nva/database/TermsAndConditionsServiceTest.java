package no.unit.nva.database;

import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static no.unit.nva.database.TermsAndConditionsService.PERSISTED_ENTITY;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TermsAndConditionsServiceTest {

    private static TermsAndConditionsService termsConditionsService;
    private static DynamoDbClient client;

    @BeforeAll
    static void initialize() {
        client = DynamoDbTestClientProvider
                .geClient();
        new DynamoDbTableCreator(client)
                .createTable(PERSISTED_ENTITY);

        termsConditionsService = new TermsAndConditionsService(client);

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
                        expectedResponse.termsConditionsUri(),
                        userIdentifier
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
                        expectedResponse.termsConditionsUri(),
                        userIdentifier
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