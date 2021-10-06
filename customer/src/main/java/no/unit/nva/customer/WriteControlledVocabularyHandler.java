package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyList, VocabularyList> {

    public WriteControlledVocabularyHandler(CustomerService customerService) {
        super(VocabularyList.class, customerService);
    }


    protected abstract CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ApiGatewayException;

    @Override
    protected final VocabularyList processInput(VocabularyList input,
                                                RequestInfo requestInfo,
                                                Context context) throws ApiGatewayException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        customer = updateVocabularySettings(input, customer);
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return VocabularyList.fromCustomerDto(updatedCustomer);
    }

}
