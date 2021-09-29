package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.UUID;

import static org.apache.http.HttpStatus.SC_OK;

public class GetCustomerHandler extends ApiGatewayHandler<Void, CustomerDtoWithoutContext> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    /**
     * Default Constructor for GetCustomerHandler.
     */
    @JacocoGenerated
    public GetCustomerHandler() {
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
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public GetCustomerHandler(CustomerService customerService, CustomerMapper customerMapper, Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        CustomerDb customerDb = customerService.getCustomer(getIdentifier(requestInfo));
        return customerMapper.toCustomerDto(customerDb);
    }

    protected UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDtoWithoutContext output) {
        return SC_OK;
    }
}
