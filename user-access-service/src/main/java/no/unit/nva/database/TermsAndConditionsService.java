package no.unit.nva.database;

import no.unit.nva.useraccessservice.dao.TermsConditions;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static nva.commons.core.attempt.Try.attempt;

public class TermsAndConditionsService {


    private static final String TABLE_NAME = new Environment()
            .readEnvOpt("NVA_ID_TYPE_TABLE_NAME")
            .orElse("TEST-PersistedObjects");
    static final URI TERMS_URL = URI.create("https://nva.sikt.no/terms/2024-10-01");

    private final SingleTableCrudService<TermsConditions> crudService;

    @JacocoGenerated
    public TermsAndConditionsService() {
        this(DEFAULT_DYNAMO_CLIENT, TABLE_NAME);
    }

    public TermsAndConditionsService(DynamoDbClient client, String tableName) {
        crudService = new SingleTableCrudService<>(client, tableName, TermsConditions.class);
    }

    public TermsConditionsResponse getTermsAndConditionsByPerson(URI cristinId) {
        var fetchedUri =
                attempt(
                        () -> TermsConditions.builder()
                                .id(cristinId)
                                .build()
                                .fetch(crudService)
                                .termsConditionsUri()
                );

        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(fetchedUri.or(() -> null).get())
                .build();
    }

    public TermsConditionsResponse updateTermsAndConditions(URI cristinId, URI termsConditions, String userId)
            throws NotFoundException {
        var upserted = TermsConditions.builder()
                .id(cristinId)
                .modifiedBy(userId)
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

    public TermsConditionsResponse getCurrentTermsAndConditions() {
        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(TERMS_URL)
                .build();
    }
}
