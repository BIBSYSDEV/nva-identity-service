package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
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

public class GetCustomerByOrgNumberHandler extends ApiGatewayHandler<Void, CustomerIdentifiers> {


    public static final String ORG_NUMBER = "orgNumber";
    private final CustomerService customerService;
    private static final Logger logger = LoggerFactory.getLogger(GetCustomerByOrgNumberHandler.class);

    /**
     * Default Constructor for GetCustomerByOrgNumberHandler.
     */
    @JacocoGenerated
    public GetCustomerByOrgNumberHandler() {
        this(defaultCustomerService(), new Environment());
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            ObjectMapperConfig.objectMapper,
            new Environment());
    }

    /**
     * Constructor for CreateCustomerbyOrgNumberHandler.
     *
     * @param customerService customerService
     * @param environment   environment
     */
    public GetCustomerByOrgNumberHandler(
        CustomerService customerService,
        Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected CustomerIdentifiers processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        long start = System.currentTimeMillis();
        String orgNumber = getOrgNumber(requestInfo);
        CustomerDto customerDto = customerService.getCustomerByOrgNumber(orgNumber);
        URI customerId = customerDto.getId();
        URI cristinId = URI.create(customerDto.getCristinId());
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
