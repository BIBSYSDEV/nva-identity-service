package no.unit.nva.customer.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.util.List;

public class GetCustomerByCristinIdHandler extends ApiGatewayHandler<Void, CustomerDto> {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    public static final String CRISTIN_ID = "cristinId";

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

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
        super(Void.class, environment);
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
            return requestInfo.getPathParameter(CRISTIN_ID);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpStatus.SC_OK;
    }
}
