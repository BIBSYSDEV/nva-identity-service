package no.unit.nva.customer.create;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class CreateCustomerDoiHandler extends CustomerDoiHandler<String> {

    private final CustomerService customerService;

    /**
     * Default Constructor for GetCustomerHandler.
     */
    @JacocoGenerated
    public CreateCustomerDoiHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     */
    public CreateCustomerDoiHandler(CustomerService customerService) {
        super(String.class);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected String processInput(String input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return customerService.createCustomerDoi(getIdentifier(requestInfo));
    }

    @Override
    protected Integer getSuccessStatusCode(String input, String output) {
        return  HttpURLConnection.HTTP_OK;
    }
}