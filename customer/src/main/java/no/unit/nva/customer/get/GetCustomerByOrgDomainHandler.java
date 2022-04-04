package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RequestUtils.getPathParameter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCustomerByOrgDomainHandler extends ApiGatewayHandlerV2<Void, CustomerIdentifiers> {

    public static final String ORG_DOMAIN = "orgDomain";
    private static final Logger logger = LoggerFactory.getLogger(GetCustomerByOrgDomainHandler.class);
    private final CustomerService customerService;

    @JacocoGenerated
    public GetCustomerByOrgDomainHandler() {
        this(defaultCustomerService());
    }

    public GetCustomerByOrgDomainHandler(CustomerService customerService) {
        super();
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, CustomerIdentifiers output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerIdentifiers processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
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
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    private String getOrgIdentifier(APIGatewayProxyRequestEvent request) {
        return getPathParameter(request, ORG_DOMAIN).orElseThrow();
    }
}