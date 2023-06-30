package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.RequestUtils;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetCustomerByCristinIdHandler extends ApiGatewayHandler<Void, CustomerDto> {

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
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(Void input, RequestInfo request, Context context)
        throws NotFoundException, InputException {
        var cristinUriIdentifier = URI.create(URLDecoder.decode(getCristinId(request), StandardCharsets.UTF_8));
        return customerService.getCustomerByCristinId(cristinUriIdentifier);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    private String getCristinId(RequestInfo request) throws InputException {
        try {
            return RequestUtils.getPathParameter(request, CRISTIN_ID).orElseThrow();
        } catch (IllegalArgumentException e) {
            throw new InputException(e.getMessage(), e);
        }
    }
}
