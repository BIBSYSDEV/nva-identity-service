package no.unit.nva.database;


import no.unit.nva.useraccessservice.dao.TermsConditions;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DynamoCrudServiceTest {

    public static final String TABLE_NAME = "nonExistentTableName";
    private static DynamoCrudService<TermsConditions> termsConditionsService;


    @BeforeAll
    static void initialize() {
        var client = DynamoDbTestClientProvider
            .geClient();
        new DynamoDbTableCreator(client)
            .createTable(TABLE_NAME);

        termsConditionsService = new DynamoCrudService<>(client, TABLE_NAME, TermsConditions.class);
    }

    @Test
    void shouldPersistPreferencesAndLicense() throws NotFoundException {
        var persistedTermsConditions = TermsConditions.builder()
            .id(randomUri())
            .modifiedBy(randomUri())
            .termsConditionsUri(randomUri())
            .build()
            .upsert(termsConditionsService);
        var persistedTwice = persistedTermsConditions
            .merge(TermsConditions.builder()
                .modifiedBy(randomUri())
                .termsConditionsUri(randomUri())
                .build())
            .upsert(termsConditionsService);

        var fetchedTermsConditions = persistedTermsConditions
            .fetch(termsConditionsService);

        assertThat(fetchedTermsConditions, is(equalTo(persistedTwice)));

    }


    @Test
    void shouldUpdateTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri();
        var termsConditionsDao = TermsConditions.builder()
            .id(userIdentifier)
            .modifiedBy(randomUri())
            .termsConditionsUri(randomUri())
            .build()
            .upsert(termsConditionsService);

        var termsConditions = TermsConditions.builder()
            .id(userIdentifier)
            .build()
            .fetch(termsConditionsService);


        assertThat(termsConditionsDao, is(equalTo(termsConditions)));
    }

    @Test
    void shouldDeleteTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri();
        var termsConditions = TermsConditions.builder()
            .id(userIdentifier)
            .modifiedBy(randomUri())
            .termsConditionsUri(randomUri())
            .build()
            .upsert(termsConditionsService);

        termsConditionsService.delete(termsConditions);

        assertThrows(NotFoundException.class,
            () -> TermsConditions.builder()
                .id(userIdentifier)
                .build()
                .fetch(termsConditionsService));
    }

    @Test
    void shouldThrowExceptionWhenFetchingNonExistentTermsConditions() {
        assertThrows(NotFoundException.class,
            () -> TermsConditions.builder()
                .id(randomUri())
                .build()
                .fetch(termsConditionsService));
    }


    @Test
    void shouldThrowExceptionWhenDeletingNonExistentTermsConditions() {
        var dao = TermsConditions.builder()
            .id(randomUri())
            .build();
        assertThrows(NotFoundException.class,
            () -> termsConditionsService.delete(dao));
    }
}