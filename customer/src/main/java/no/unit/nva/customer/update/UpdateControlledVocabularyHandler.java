package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import no.unit.nva.customer.create.CreateControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

public class UpdateControlledVocabularyHandler extends CreateControlledVocabularyHandler {

    public static final String VOCABULARY_SETTINGS_NOT_DEFINED_ERROR = "Customer had not defined Vocabulary settings."
        + " Use POST if you want to define new";

    @JacocoGenerated
    public UpdateControlledVocabularyHandler() {
        this(defaultCustomerService(), new Environment());
    }

    public UpdateControlledVocabularyHandler(CustomerService customerService, Environment environment) {
        super(customerService, environment);
    }

    @Override
    protected CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws BadRequestException {
        if (!customer.getVocabularies().isEmpty()) {
            return customer.copy().withVocabularies(input.getVocabularies()).build();
        }
        throw new BadRequestException(VOCABULARY_SETTINGS_NOT_DEFINED_ERROR);
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularyList input, VocabularyList output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}
