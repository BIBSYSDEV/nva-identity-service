package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RequestUtils.getPathParameter;

public class GetCustomerByOrgDomainHandler extends ApiGatewayHandler<Void, CustomerIdentifiers> {

    public static final String ORG_DOMAIN = "orgDomain";
    private static final Logger logger = LoggerFactory.getLogger(GetCustomerByOrgDomainHandler.class);
    private final CustomerService customerService;

    @JacocoGenerated
    public GetCustomerByOrgDomainHandler() {
        this(defaultCustomerService());
    }

    public GetCustomerByOrgDomainHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected CustomerIdentifiers processInput(Void input, RequestInfo requestInfo, Context context)
        throws NotFoundException {
        long start = System.currentTimeMillis();
        String orgDomain = getOrgIdentifier(requestInfo);
        CustomerDto customerDto = customerService.getCustomerByOrgDomain(orgDomain);
        URI customerId = customerDto.getId();
        URI cristinId = customerDto.getCristinId();
        long stop = System.currentTimeMillis();
        logger.info("processInput took {} ms", stop - start);
        return new CustomerIdentifiers(customerId, cristinId);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CustomerIdentifiers output) {
        return HttpURLConnection.HTTP_OK;
    }

    private String getOrgIdentifier(RequestInfo request) {
        return getPathParameter(request, ORG_DOMAIN).orElseThrow();
    }
}
