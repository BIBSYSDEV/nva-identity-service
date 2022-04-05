package no.unit.nva.customer.create;

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
        super();
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, CustomerDto output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    protected CustomerDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        var request = CreateCustomerRequest.fromJson(input);
        return customerService.createCustomer(request.toCustomerDto());
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
