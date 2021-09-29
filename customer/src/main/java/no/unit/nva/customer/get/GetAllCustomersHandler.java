package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerList;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;

public class GetAllCustomersHandler extends ApiGatewayHandler<Void, CustomerList> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    /**
     * Default Constructor for GetAllCustomersHandler.
     */
    @JacocoGenerated
    public GetAllCustomersHandler() {
        this(defaultCustomerService(),
            defaultCustomerMapper(),
            new Environment()
        );
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            ObjectMapperConfig.objectMapper,
            new Environment());
    }

    @JacocoGenerated
    private static CustomerMapper defaultCustomerMapper() {
        String namespace = new Environment().readEnv(ID_NAMESPACE_ENV);
        return new CustomerMapper(namespace);
    }

    /**
     * Constructor for CreateAllCustomersHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public GetAllCustomersHandler(
        CustomerService customerService,
        CustomerMapper customerMapper,
        Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    protected CustomerList processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        List<CustomerDb> customerDbs = customerService.getCustomers();
        return customerMapper.toCustomerListFromCustomerDbs(customerDbs);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerList output) {
        return SC_OK;
    }
}
