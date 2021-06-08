package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.RequestUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCustomerByCristinIdHandler extends ApiGatewayHandler<Void, CustomerDto> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String CRISTIN_ID = "cristinId";

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private static final Logger logger = LoggerFactory.getLogger(GetCustomerByCristinIdHandler.class);

    /**
     * Default Constructor for GetCustomerByCristinIdHandler.
     */
    @JacocoGenerated
    public GetCustomerByCristinIdHandler() {
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
     * Constructor for GetCustomerByCristinIdHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public GetCustomerByCristinIdHandler(
        CustomerService customerService,
        CustomerMapper customerMapper,
        Environment environment) {
        super(Void.class, environment, logger);
        this.customerService = customerService;
        this.customerMapper = customerMapper;

    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        String cristinId = getCristinId(requestInfo);
        CustomerDb customerDb = customerService.getCustomerByCristinId(cristinId);

        return customerMapper.toCustomerDto(customerDb);
    }

    private String getCristinId(RequestInfo requestInfo) throws InputException {
        try {
            return RequestUtils.getPathParameter(requestInfo, CRISTIN_ID);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpStatus.SC_OK;
    }
}
