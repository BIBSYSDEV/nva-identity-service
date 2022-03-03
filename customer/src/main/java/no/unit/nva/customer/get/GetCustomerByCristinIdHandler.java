package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.core.JacocoGenerated;

public class GetCustomerByCristinIdHandler extends ApiGatewayHandlerV2<Void, CustomerDto> {

    public static final String CRISTIN_ID = "cristinId";

    private final CustomerService customerService;

    @JacocoGenerated
    public GetCustomerByCristinIdHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for GetCustomerByCristinIdHandler.
     *
     * @param customerService customerService
     */
    public GetCustomerByCristinIdHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(String input, APIGatewayProxyRequestEvent request, Context context) {
        String cristinId = getCristinId(request);
        return customerService.getCustomerByCristinId(cristinId);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    private String getCristinId(APIGatewayProxyRequestEvent request) throws InputException {
        try {
            return request.getPathParameters().get(CRISTIN_ID);
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }
}
