package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.List;

import static no.unit.nva.customer.Constants.defaultCustomerService;

public class GetCustomerHandler extends CustomerHandler<Void> {

    private final CustomerService customerService;

    /**
     * Default Constructor for ListCustomerHandler.
     */
    @JacocoGenerated
    public GetCustomerHandler() {
        this(defaultCustomerService(), new Environment());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     * @param environment
     */
    public GetCustomerHandler(CustomerService customerService, Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws InputException, NotFoundException, ForbiddenException {
        return customerService.getCustomer(getIdentifier(requestInfo));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
