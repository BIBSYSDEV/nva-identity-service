package no.unit.nva.customer.get;

import static org.apache.http.HttpStatus.SC_OK;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetAllCustomersHandler extends ApiGatewayHandler<Void, CustomerList> {

    private final CustomerService customerService;

    /**
     * Default Constructor for GetAllCustomersHandler.
     */
    @JacocoGenerated
    public GetAllCustomersHandler() {
        this(defaultCustomerService(), new Environment());
    }

    /**
     * Constructor for CreateAllCustomersHandler.
     *
     * @param customerService customerService
     * @param environment     environment
     */
    public GetAllCustomersHandler(CustomerService customerService, Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected CustomerList processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        List<CustomerDto> customers = customerService.getCustomers();
        return CustomerList.of(customers);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerList output) {
        return SC_OK;
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            ObjectMapperConfig.objectMapper,
            new Environment());
    }
}
