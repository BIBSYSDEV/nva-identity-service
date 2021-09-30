package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
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

public class GetCustomerByCristinIdHandler extends ApiGatewayHandler<Void, CustomerDto> {

    public static final String CRISTIN_ID = "cristinId";

    private final CustomerService customerService;

    @JacocoGenerated
    public GetCustomerByCristinIdHandler() {
        this(defaultCustomerService(),
             new Environment()
        );
    }

    /**
     * Constructor for GetCustomerByCristinIdHandler.
     *
     * @param customerService customerService
     * @param environment     environment
     */
    public GetCustomerByCristinIdHandler(
        CustomerService customerService,
        Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        String cristinId = getCristinId(requestInfo);
        return customerService.getCustomerByCristinId(cristinId);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpStatus.SC_OK;
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
            AmazonDynamoDBClientBuilder.defaultClient(),
            ObjectMapperConfig.objectMapper,
            new Environment());
    }

    private String getCristinId(RequestInfo requestInfo) throws InputException {
        try {
            return requestInfo.getPathParameter(CRISTIN_ID);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }
}
