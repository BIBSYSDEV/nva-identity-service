package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.JacocoGenerated;

public class GetAllCustomersHandler extends ApiGatewayHandler<Void, CustomerList> {

    private final CustomerService customerService;

    /**
     * Default Constructor for GetAllCustomersHandler.
     */
    @JacocoGenerated
    public GetAllCustomersHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for CreateAllCustomersHandler.
     *
     * @param customerService customerService
     */
    public GetAllCustomersHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerList processInput(Void input, RequestInfo requestInfo, Context context) {
        List<CustomerDto> customers = customerService.getCustomers();
        return new CustomerList(customers);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
