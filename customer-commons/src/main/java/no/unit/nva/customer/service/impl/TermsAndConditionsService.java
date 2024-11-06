package no.unit.nva.customer.service.impl;

import no.unit.nva.customer.model.TermsConditions;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static no.unit.nva.customer.Constants.PERSISTED_ENTITY;
import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

public class TermsAndConditionsService {

    private static final URI TERMS_URL = URI.create("https://nva.sikt.no/terms/2024-10-01");
    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2024, 10, 1, 0, 0, 0);

    private final DynamoCrudService<TermsConditions> crudService;


    public TermsAndConditionsService() {
        this(DEFAULT_DYNAMO_CLIENT);
    }

    public TermsAndConditionsService(DynamoDbClient client) {
        crudService = new DynamoCrudService<>(client, PERSISTED_ENTITY, TermsConditions.class);

    }

    public TermsConditions getCurrentTermsAndConditions() {
        return TermsConditions.builder()
                .withId(TERMS_URL)
                .modified(VALID_FROM.toInstant(ZonedDateTime.now().getOffset()))
                .build();
    }

    public List<TermsConditions> getAllTermsAndConditions() {
        return List.of(getCurrentTermsAndConditions());
    }
}
