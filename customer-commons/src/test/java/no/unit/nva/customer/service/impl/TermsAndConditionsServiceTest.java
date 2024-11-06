package no.unit.nva.customer.service.impl;


import no.unit.nva.customer.model.TermsConditions;
import no.unit.nva.customer.testing.DynamoDbTableCreator;
import no.unit.nva.customer.testing.DynamoDbTestClientProvider;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TermsAndConditionsServiceTest {

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
    void shouldUpdateTermsConditions() throws NotFoundException {
        var userIdentifier = randomUri();
        var termsConditionsDao = TermsConditions.builder()
                .withId(userIdentifier)
                .termsConditionsUri(randomUri())
                .build()
                .upsert(termsConditionsService);

        var termsConditions = TermsConditions.builder()
                .withId(userIdentifier)
                .build()
                .fetch(termsConditionsService);


        assertThat(termsConditionsDao, is(equalTo(termsConditions)));
    }

    @Test
    void shouldThrowExceptionWhenFetchingNonExistentTermsConditions() {
        assertThrows(NotFoundException.class,
                () -> TermsConditions.builder()
                        .withId(randomUri())
                        .build()
                        .fetch(termsConditionsService));
    }

}