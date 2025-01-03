package no.unit.nva.database;

import java.util.Optional;
import no.unit.nva.useraccessservice.dao.TermsConditions;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;

public class TermsAndConditionsService {

    static final URI TERMS_URL = URI.create("https://nva.sikt.no/terms/2024-10-01");
    public static final String TERMS_TABLE_NAME_ENV = "TERMS_TABLE_NAME";

    private final SingleTableCrudService<TermsConditions> crudService;

    @JacocoGenerated
    public TermsAndConditionsService() {
        this(DEFAULT_DYNAMO_CLIENT, new Environment());
    }

    public TermsAndConditionsService(DynamoDbClient client, Environment environment) {
        crudService = new SingleTableCrudService<>(client, environment.readEnv(TERMS_TABLE_NAME_ENV),
                                                   TermsConditions.class);
    }

    public TermsConditionsResponse getTermsAndConditionsByPerson(URI cristinPersonId) {
        return Optional.of(cristinPersonId)
                   .map(id -> {
                       try {
                           return TermsConditions.builder()
                                      .id(id.toString())
                                      .build()
                                      .fetch(crudService)
                                      .termsConditionsUri();
                       } catch (NotFoundException e) {
                           return null;
                       }
                   })
                   .map(fetchedUri -> TermsConditionsResponse.builder()
                                          .withTermsConditionsUri(fetchedUri)
                                          .build())
                   .orElse(null);
    }

    public TermsConditionsResponse updateTermsAndConditions(URI cristinPersonId, URI termsConditions, String userId)
        throws NotFoundException {
        var upserted = TermsConditions.builder()
                           .id(cristinPersonId.toString())
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
