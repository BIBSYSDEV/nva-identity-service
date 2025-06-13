package no.unit.nva.customer.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RequestUtils.getIdentifier;
import static nva.commons.apigateway.AccessRight.MANAGE_CHANNEL_CLAIMS;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class CreateChannelClaimHandler extends ApiGatewayHandler<ChannelClaimRequest, Void> {

    private final CustomerService customerService;

    @JacocoGenerated
    public CreateChannelClaimHandler() {
        this(defaultCustomerService(), new Environment());
    }

    public CreateChannelClaimHandler(CustomerService customerService, Environment environment) {
        super(ChannelClaimRequest.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected void validateRequest(ChannelClaimRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        if (requestInfo.userIsAuthorized(MANAGE_CHANNEL_CLAIMS)) {
            return;
        }
        throw new ForbiddenException();
    }

    @Override
    protected Void processInput(ChannelClaimRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var customerIdentifier = getIdentifier(requestInfo);
        customerService.createChannelClaim(customerIdentifier, request.toDto());
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ChannelClaimRequest request, Void response) {
        return HTTP_CREATED;
    }
}
