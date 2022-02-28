package no.unit.nva.customer.update;

import java.net.HttpURLConnection;
import no.unit.nva.customer.create.CreateControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyListDto;
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
    protected CustomerDto updateVocabularySettings(VocabularyListDto input, CustomerDto customer)
        throws ApiGatewayException {
        if (!customer.getVocabularies().isEmpty()) {
            customer.setVocabularies(input.getVocabularies());
            return customer;
        }
        throw new NotFoundException(VOCABULARY_SETTINGS_NOT_DEFINED_ERROR);
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularyListDto input, VocabularyListDto output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
