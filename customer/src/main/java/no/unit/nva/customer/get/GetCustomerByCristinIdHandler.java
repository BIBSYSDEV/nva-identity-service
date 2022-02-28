package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.responses.CustomerResponse;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.util.List;

import static no.unit.nva.customer.Constants.defaultCustomerService;

public class GetCustomerByCristinIdHandler extends ApiGatewayHandler<Void, CustomerResponse> {

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
    protected CustomerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        String cristinId = getCristinId(requestInfo);
        return CustomerResponse.toCustomerResponse(customerService.getCustomerByCristinId(cristinId));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerResponse output) {
        return HttpStatus.SC_OK;
    }

    private String getCristinId(RequestInfo requestInfo) throws InputException {
        try {
            return requestInfo.getPathParameter(CRISTIN_ID);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }
}
