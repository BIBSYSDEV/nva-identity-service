package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.UUID;

import static no.unit.nva.customer.Constants.defaultCustomerService;

public class GetControlledVocabularyHandler extends ControlledVocabularyHandler<Void, VocabularyList> {

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(customerService, Void.class);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularyList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected VocabularyList processInput(Void input, RequestInfo requestInfo, Context context)
            throws NotFoundException, ForbiddenException {

        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customerDto = customerService.getCustomer(identifier);
        return VocabularyList.fromCustomerDto(customerDto);
    }
}
