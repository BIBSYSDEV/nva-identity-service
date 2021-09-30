package no.unit.nva.customer.create;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.util.List;

public class CreateCustomerHandler extends ApiGatewayHandler<CustomerDto, CustomerDto> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    /**
     * Default Constructor for CreateCustomerHandler.
     */
    @JacocoGenerated
    public CreateCustomerHandler() {
        this(defaultCustomerService(),
            defaultCustomerMapper(),
            new Environment())
        ;
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
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public CreateCustomerHandler(
        CustomerService customerService,
        CustomerMapper customerMapper,
        Environment environment) {
        super(CustomerDto.class, environment);
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    protected CustomerDto processInput(
            CustomerDto input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        CustomerDb customerDb = customerMapper.toCustomerDb(input);
        CustomerDb createdCustomerDb = customerService.createCustomer(customerDb);
        return customerMapper.toCustomerDto(createdCustomerDb);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerDto input, CustomerDto output) {
        return HttpStatus.SC_CREATED;
    }
}
