package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
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

public class GetCustomerByOrgNumberHandler extends ApiGatewayHandler<Void, CustomerIdentifiers> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String ORG_NUMBER = "orgNumber";

    private final CustomerMapper customerMapper;
    private final CustomerService customerService;
    private static final Logger logger = LoggerFactory.getLogger(GetCustomerByOrgNumberHandler.class);

    /**
     * Default Constructor for GetCustomerByOrgNumberHandler.
     */
    @JacocoGenerated
    public GetCustomerByOrgNumberHandler() {
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
     * Constructor for CreateCustomerbyOrgNumberHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public GetCustomerByOrgNumberHandler(
        CustomerService customerService,
        CustomerMapper customerMapper,
        Environment environment) {
        super(Void.class, environment, logger);
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    protected CustomerIdentifiers processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        long start = System.currentTimeMillis();
        String orgNumber = getOrgNumber(requestInfo);
        CustomerDb customerDb = customerService.getCustomerByOrgNumber(orgNumber);
        CustomerDto customerDto = customerMapper.toCustomerDto(customerDb);
        URI customerId = customerDto.getId();
        URI cristinId = URI.create(customerDb.getCristinId());
        long stop = System.currentTimeMillis();
        logger.info("processInput took {} ms", stop - start);
        return new CustomerIdentifiers(customerId, cristinId);
    }

    private String getOrgNumber(RequestInfo requestInfo) throws InputException {
        try {
            return RequestUtils.getPathParameter(requestInfo, ORG_NUMBER);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerIdentifiers output) {
        return HttpStatus.SC_OK;
    }
}
