package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.JacocoGenerated;

public class GetCustomerHandler extends CustomerHandler<Void> {

    private final CustomerService customerService;

    /**
     * Default Constructor for GetCustomerHandler.
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
        super();
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        return customerService.getCustomer(getIdentifier(requestInfo));
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
