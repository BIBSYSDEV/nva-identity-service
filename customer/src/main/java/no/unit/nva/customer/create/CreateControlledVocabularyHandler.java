package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import java.net.HttpURLConnection;
import no.unit.nva.customer.WriteControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.exceptions.BadRequestException;
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
    protected CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ConflictException, BadRequestException {
        if (customer.getVocabularies().isEmpty()) {
            return customer.copy().withVocabularies(input.getVocabularies()).build();
        }
        throw new ConflictException(CUSTOMER_SETTINGS_EXIST_ERROR);
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularyList input, VocabularyList output) {
        return HttpURLConnection.HTTP_CREATED;
    }
}
