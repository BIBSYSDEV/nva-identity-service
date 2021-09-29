package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

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
        super(Void.class, environment);
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    protected CustomerIdentifiers processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        long start = System.currentTimeMillis();
        String orgNumber = getOrgNumber(requestInfo);
        CustomerDb customerDb = customerService.getCustomerByOrgNumber(orgNumber);
        CustomerDtoWithoutContext customerDto = customerMapper.toCustomerDtoWithoutContext(customerDb);
        URI customerId = customerDto.getId();
        URI cristinId = URI.create(customerDb.getCristinId());
        long stop = System.currentTimeMillis();
        logger.info("processInput took {} ms", stop - start);
        return new CustomerIdentifiers(customerId, cristinId);
    }

    private String getOrgNumber(RequestInfo requestInfo) throws InputException {
        try {
            return requestInfo.getPathParameter(ORG_NUMBER);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerIdentifiers output) {
        return HttpStatus.SC_OK;
    }
}
