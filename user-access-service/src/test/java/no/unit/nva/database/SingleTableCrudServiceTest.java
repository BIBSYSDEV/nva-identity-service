package no.unit.nva.database;


import no.unit.nva.useraccessservice.dao.TermsConditions;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SingleTableCrudServiceTest {

    private static SingleTableCrudService<TermsConditions> singleTableCrudService;


    @BeforeAll
    static void initialize() {

        var client = DatabaseTestConfig
                .getEmbeddedClient();
        var tableName = "PersistedObjectsTable";
        new SingleTableTemplateCreator(client)
                .createTable(tableName);

        singleTableCrudService = new SingleTableCrudService<>(client, tableName, TermsConditions.class);
    }

    @Test
    void shouldPersistPreferencesAndLicense() throws NotFoundException {

        var persistedTermsConditions = TermsConditions.builder()
                .id(randomUri().toString())
                .modifiedBy(randomString())
                .termsConditionsUri(randomUri())
                .build()
                .upsert(singleTableCrudService);

        var persistedTwice = persistedTermsConditions
                .merge(TermsConditions.builder()
                        .modifiedBy(randomString())
                        .termsConditionsUri(randomUri())
                        .build())
                .upsert(singleTableCrudService);

        var fetchedTermsConditions = persistedTermsConditions
                .fetch(singleTableCrudService);

        assertThat(fetchedTermsConditions, is(equalTo(persistedTwice)));
    }


    @Test
    void shouldUpdateTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri().toString();
        var termsConditionsDao = TermsConditions.builder()
                .id(userIdentifier)
                .modifiedBy(randomString())
                .termsConditionsUri(randomUri())
                .build()
                .upsert(singleTableCrudService);

        var termsConditions = TermsConditions.builder()
                .id(userIdentifier)
                .build()
                .fetch(singleTableCrudService);


        assertThat(termsConditionsDao, is(equalTo(termsConditions)));
    }

    @Test
    void shouldDeleteTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri().toString();
        var termsConditions = TermsConditions.builder()
                .id(userIdentifier)
                .modifiedBy(randomString())
                .termsConditionsUri(randomUri())
                .build()
                .upsert(singleTableCrudService);

        singleTableCrudService.delete(termsConditions);

        assertThrows(NotFoundException.class,
                () -> TermsConditions.builder()
                        .id(userIdentifier)
                        .build()
                        .fetch(singleTableCrudService));
    }

    @Test
    void shouldThrowExceptionWhenFetchingNonExistentTermsConditions() {
        assertThrows(NotFoundException.class,
                () -> TermsConditions.builder()
                        .id(randomUri().toString())
                        .build()
                        .fetch(singleTableCrudService));
    }


    @Test
    void shouldThrowExceptionWhenDeletingNonExistentTermsConditions() {
        var dao = TermsConditions.builder()
                .id(randomUri().toString())
                .build();
        assertThrows(NotFoundException.class,
                () -> singleTableCrudService.delete(dao));
    }
}