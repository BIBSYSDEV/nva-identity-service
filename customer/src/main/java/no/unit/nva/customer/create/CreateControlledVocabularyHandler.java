package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CreateControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularySettingsList, VocabularySettingsList> {

    @JacocoGenerated
    public CreateControlledVocabularyHandler(){
        this(defaultCustomerService());
    }

    public CreateControlledVocabularyHandler(CustomerService customerService) {
        super(VocabularySettingsList.class,customerService);
    }

    @Override
    protected VocabularySettingsList processInput(VocabularySettingsList input,
                                                  RequestInfo requestInfo,
                                                  Context context) throws ApiGatewayException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        customer.setVocabularySettings(input.getVocabularySettings());
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return new VocabularySettingsList(updatedCustomer.getVocabularySettings());
    }

    private UUID extractIdentifier(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER))
            .map(UUID::fromString)
            .orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(VocabularySettingsList input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_CREATED;
    }
}
