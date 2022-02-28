package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;

import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyListDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyListDto, VocabularyListDto> {

    public WriteControlledVocabularyHandler(CustomerService customerService) {
        super(VocabularyListDto.class, customerService);
    }


    protected abstract CustomerDto updateVocabularySettings(VocabularyListDto input, CustomerDto customer)
        throws ApiGatewayException;

    @Override
    protected final VocabularyListDto processInput(VocabularyListDto input,
                                                   RequestInfo requestInfo,
                                                   Context context) throws ApiGatewayException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier).toCustomerDto();
        customer = updateVocabularySettings(input, customer);
        CustomerDao updatedCustomer = customerService.updateCustomer(identifier, CustomerDao.fromCustomerDto(customer));
        return VocabularyListDto.fromCustomerDto(updatedCustomer.toCustomerDto());
    }

}
