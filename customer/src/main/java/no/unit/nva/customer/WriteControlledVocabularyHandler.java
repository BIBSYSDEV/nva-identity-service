package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyList, VocabularyList> {

    protected WriteControlledVocabularyHandler(CustomerService customerService) {
        super(customerService,VocabularyList.class);
    }

    protected abstract CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ConflictException, NotFoundException;

    @Override
    protected final VocabularyList processInput(VocabularyList input,
                                                RequestInfo requestInfo,
                                                Context context)
        throws InputException, NotFoundException, ConflictException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        customer = updateVocabularySettings(input, customer);
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return VocabularyList.fromCustomerDto(updatedCustomer);
    }
}
