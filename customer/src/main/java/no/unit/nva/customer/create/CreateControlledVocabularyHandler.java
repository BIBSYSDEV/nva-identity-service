package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.WriteControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

public class CreateControlledVocabularyHandler extends WriteControlledVocabularyHandler {

    public static final String CUSTOMER_SETTINGS_EXIST_ERROR = "Customer has already defined Vocabulary settings";

    @JacocoGenerated
    public CreateControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public CreateControlledVocabularyHandler(CustomerService customerService) {
        super(customerService);
    }

    @Override
    protected CustomerDto updateVocabularySettings(VocabularySettingsList input, CustomerDto customer)
        throws ApiGatewayException {
        if (customer.getVocabularySettings().isEmpty()) {
            return customer.copy().withVocabularySettings(input.getVocabularySettings()).build();
        }
        throw new ConflictException(CUSTOMER_SETTINGS_EXIST_ERROR);
    }



    @Override
    protected Integer getSuccessStatusCode(VocabularySettingsList input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_CREATED;
    }
}
