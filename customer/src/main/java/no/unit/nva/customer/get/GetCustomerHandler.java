package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetCustomerHandler extends CustomerHandler<Void> {

    private final CustomerService customerService;

    /**
     * Default Constructor for ListCustomerHandler.
     */
    @JacocoGenerated
    public GetCustomerHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     */
    public GetCustomerHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws InputException, NotFoundException, ForbiddenException {
        if (!userIsAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
        return customerService.getCustomer(getIdentifier(requestInfo));
    }

    private boolean userIsAuthorized(RequestInfo requestInfo) {
        return requestInfo.clientIsInternalBackend()
               || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
