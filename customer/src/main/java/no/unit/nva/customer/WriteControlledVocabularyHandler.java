package no.unit.nva.customer;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyList, VocabularyList> {

    protected WriteControlledVocabularyHandler(CustomerService customerService) {
        super(customerService, VocabularyList.class);
    }

    protected abstract CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ApiGatewayException;

    @Override
    protected final VocabularyList processInput(VocabularyList input,
                                                RequestInfo requestInfo,
                                                Context context)
        throws ApiGatewayException {
        if (!userIsAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customer = customerService.getCustomer(identifier);
        customer = updateVocabularySettings(input, customer);
        CustomerDto updatedCustomer = customerService.updateCustomer(identifier, customer);
        return VocabularyList.fromCustomerDto(updatedCustomer);
    }

    private boolean userIsAuthorized(RequestInfo requestInfo) {
        return requestInfo.clientIsInternalBackend()
               || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS)
               || requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION);
    }
}
