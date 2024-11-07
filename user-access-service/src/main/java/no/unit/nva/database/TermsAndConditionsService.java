package no.unit.nva.database;


import no.unit.nva.useraccessservice.dao.TermsConditions;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;


import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

public class TermsAndConditionsService {

    public static final String PERSISTED_ENTITY = "PersistedEntity";
    private static final URI TERMS_URL = URI.create("https://nva.sikt.no/terms/2024-10-01");

    private final DynamoCrudService<TermsConditions> crudService;


    public TermsAndConditionsService() {
        this(DEFAULT_DYNAMO_CLIENT);
    }

    public TermsAndConditionsService(DynamoDbClient client) {
        crudService = new DynamoCrudService<>(client, PERSISTED_ENTITY, TermsConditions.class);
    }

    public TermsConditionsResponse getCurrentTermsAndConditions() {
        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(TERMS_URL)
                .build();
    }

    public TermsConditionsResponse getTermsAndConditionsByPerson(URI cristinId) throws NotFoundException {
        var fetched = TermsConditions.builder()
                .withId(cristinId)
                .build()
                .fetch(crudService);
        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(fetched.termsConditionsUri())
                .build();
    }

    public TermsConditionsResponse updateTermsAndConditions(URI cristinId, URI termsConditions) throws NotFoundException {
        var upserted = TermsConditions.builder()
                .withId(cristinId)
                .termsConditionsUri(termsConditions)
                .build()
                .upsert(crudService);
        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(upserted.termsConditionsUri())
                .build();
    }

    public List<TermsConditionsResponse> getAllTermsAndConditions() {
        return List.of(getCurrentTermsAndConditions());
    }
}
