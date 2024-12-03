package no.unit.nva.database;


import no.unit.nva.useraccessservice.dao.TermsConditions;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DynamoCrudServiceTest {

    private static DynamoCrudService<TermsConditions> dynamoCrudService;


    @BeforeAll
    static void initialize() {

        var client = DatabaseTestConfig
                .getEmbeddedClient();
        var tableName = new Environment()
                .readEnv("NVA_ID_TYPE_TABLE_NAME");
        new DynamoDbTableCreator(client)
                .createTable(tableName);

        dynamoCrudService = new DynamoCrudService<>(client, tableName, TermsConditions.class);
    }

    @Test
    void shouldPersistPreferencesAndLicense() throws NotFoundException {
        var persistedTermsConditions = TermsConditions.builder()
                .id(randomUri())
                .modifiedBy(randomUri())
                .termsConditionsUri(randomUri())
                .build()
                .upsert(dynamoCrudService);
        var persistedTwice = persistedTermsConditions
                .merge(TermsConditions.builder()
                        .modifiedBy(randomUri())
                        .termsConditionsUri(randomUri())
                        .build())
                .upsert(dynamoCrudService);

        var fetchedTermsConditions = persistedTermsConditions
                .fetch(dynamoCrudService);

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
                .upsert(dynamoCrudService);

        var termsConditions = TermsConditions.builder()
                .id(userIdentifier)
                .build()
                .fetch(dynamoCrudService);


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
                .upsert(dynamoCrudService);

        dynamoCrudService.delete(termsConditions);

        assertThrows(NotFoundException.class,
                () -> TermsConditions.builder()
                        .id(userIdentifier)
                        .build()
                        .fetch(dynamoCrudService));
    }

    @Test
    void shouldThrowExceptionWhenFetchingNonExistentTermsConditions() {
        assertThrows(NotFoundException.class,
                () -> TermsConditions.builder()
                        .id(randomUri())
                        .build()
                        .fetch(dynamoCrudService));
    }


    @Test
    void shouldThrowExceptionWhenDeletingNonExistentTermsConditions() {
        var dao = TermsConditions.builder()
                .id(randomUri())
                .build();
        assertThrows(NotFoundException.class,
                () -> dynamoCrudService.delete(dao));
    }
}