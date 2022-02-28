package no.unit.nva.customer.create;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.requests.CreateCustomerRequest;
import no.unit.nva.customer.model.responses.CustomerResponse;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.util.List;

import static no.unit.nva.customer.Constants.defaultCustomerService;

public class CreateCustomerHandler extends ApiGatewayHandler<CreateCustomerRequest, CustomerResponse> {

    private final CustomerService customerService;

    /**
     * Default Constructor for CreateCustomerHandler.
     */
    @JacocoGenerated
    public CreateCustomerHandler() {
        this(defaultCustomerService(), new Environment());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     * @param environment     environment
     */
    public CreateCustomerHandler(CustomerService customerService, Environment environment) {
        super(CreateCustomerRequest.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected CustomerResponse processInput(CreateCustomerRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        CustomerDao customer = customerService.createCustomer(input.toCustomerDao());
        return CustomerResponse.toCustomerResponse(customer);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateCustomerRequest input, CustomerResponse output) {
        return HttpStatus.SC_CREATED;
    }
}
