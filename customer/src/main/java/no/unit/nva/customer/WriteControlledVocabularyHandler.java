package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularySettingsList, VocabularySettingsList> {

    public WriteControlledVocabularyHandler(CustomerService customerService) {
        super(VocabularySettingsList.class, customerService);
    }


    protected abstract CustomerDto updateVocabularySettings(VocabularySettingsList input, CustomerDto customer)
        throws ApiGatewayException;

    @Override
    protected final VocabularySettingsList processInput(VocabularySettingsList input,
                                                  RequestInfo requestInfo,
                                                  Context context) throws ApiGatewayException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        customer = updateVocabularySettings(input, customer);
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return new VocabularySettingsList(updatedCustomer.getVocabularySettings());
    }

}
