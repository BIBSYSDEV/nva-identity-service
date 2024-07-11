package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class CreateCustomerHandler extends CustomerHandler<CreateCustomerRequest> {

    private final CustomerService customerService;

    /**
     * Default Constructor for CreateCustomerHandler.
     */
    @JacocoGenerated
    public CreateCustomerHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     */
    public CreateCustomerHandler(CustomerService customerService) {
        super(CreateCustomerRequest.class);
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateCustomerRequest input, CustomerDto output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    protected CustomerDto processInput(CreateCustomerRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {



        return customerService.createCustomer(input.toCustomerDto());
    }

    private boolean userIsAuthorized(RequestInfo requestInfo) {
        return requestInfo.clientIsInternalBackend()
               || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(CreateCustomerRequest createCustomerRequest, RequestInfo requestInfo,
                                   Context context) throws ApiGatewayException {
        if (!userIsAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }

    }
}
