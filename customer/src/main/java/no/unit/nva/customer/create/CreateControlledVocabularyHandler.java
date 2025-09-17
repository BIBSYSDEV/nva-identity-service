package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.WriteControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import java.net.HttpURLConnection;
import nva.commons.core.JacocoGenerated;

public class CreateControlledVocabularyHandler extends WriteControlledVocabularyHandler {

    public static final String CUSTOMER_SETTINGS_EXIST_ERROR = "Customer has already defined Vocabulary settings";

    @JacocoGenerated
    public CreateControlledVocabularyHandler() {
        this(defaultCustomerService(),  new Environment());
    }

    public CreateControlledVocabularyHandler(CustomerService customerService, Environment environment) {
        super(customerService, environment);
    }

    @Override
    protected CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ConflictException, BadRequestException {
        if (customer.getVocabularies().isEmpty()) {
            return customer.copy().withVocabularies(input.getVocabularies()).build();
        }
        throw new ConflictException(CUSTOMER_SETTINGS_EXIST_ERROR);
    }

    @Override
    protected void validateRequest(VocabularyList vocabularyList, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularyList input, VocabularyList output) {
        return HttpURLConnection.HTTP_CREATED;
    }
}
