package no.unit.nva.customer.update;

import java.net.HttpURLConnection;
import no.unit.nva.customer.create.CreateControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class UpdateControlledVocabularyHandler extends CreateControlledVocabularyHandler {

    public static final String VOCABULARY_SETTINGS_NOT_DEFINED_ERROR = "Customer had not defined Vocabulary settings."
                                                                       + " Use POST if you want to define new";

    @JacocoGenerated
    public UpdateControlledVocabularyHandler() {
        super();
    }

    public UpdateControlledVocabularyHandler(DynamoDBCustomerService customerService) {
        super(customerService);
    }

    @Override
    protected CustomerDto updateVocabularySettings(VocabularySettingsList input, CustomerDto customer)
        throws ApiGatewayException {
        if (!customer.getVocabularySettings().isEmpty()) {
            return customer.copy().withVocabularySettings(input.getVocabularySettings()).build();
        }
        throw new NotFoundException(VOCABULARY_SETTINGS_NOT_DEFINED_ERROR);
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularySettingsList input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
