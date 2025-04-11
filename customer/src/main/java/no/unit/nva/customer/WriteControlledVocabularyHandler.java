package no.unit.nva.customer;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;

import java.util.UUID;
import nva.commons.core.Environment;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;

public abstract class WriteControlledVocabularyHandler
    extends ControlledVocabularyHandler<VocabularyList, VocabularyList> {

    protected WriteControlledVocabularyHandler(CustomerService customerService, Environment environment) {
        super(customerService, VocabularyList.class, environment);
    }

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

    protected abstract CustomerDto updateVocabularySettings(VocabularyList input, CustomerDto customer)
        throws ApiGatewayException;

    private boolean userIsAuthorized(RequestInfo requestInfo) {
        return requestInfo.clientIsInternalBackend()
            || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS)
            || requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION);
    }
}
