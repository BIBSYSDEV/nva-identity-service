package no.unit.nva.customer.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.RequestUtils;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class CreateChannelClaimHandler extends ApiGatewayHandler<ChannelClaimRequest, Void> {

    private static final String IDENTIFIER_PARAMETER = "identifier";
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
        if (userIsAuthorizedOnCustomer(requestInfo)) {
            return;
        }
        throw new ForbiddenException();
    }

    @Override
    protected Void processInput(ChannelClaimRequest request, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var customerIdentifier = extractIdentifierFromPath(requestInfo);
        customerService.createChannelClaim(customerIdentifier, request.toDto());
//        return new ChannelClaimResponse();
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(ChannelClaimRequest request, Void response) {
        return HTTP_CREATED;
    }

    private boolean userIsAuthorizedOnCustomer(RequestInfo requestInfo) throws UnauthorizedException {
        var customerIdentifierFromPath = extractIdentifierFromPath(requestInfo).toString();
        var customerIdentifierFromUser = UriWrapper.fromUri(requestInfo.getCurrentCustomer()).getLastPathElement();

        return customerIdentifierFromPath.equals(customerIdentifierFromUser)
               && requestInfo.userIsAuthorized(MANAGE_RESOURCES_ALL);
    }

    private UUID extractIdentifierFromPath(RequestInfo requestInfo) {
        return RequestUtils.getPathParameter(requestInfo, IDENTIFIER_PARAMETER)
                   .map(UUID::fromString)
                   .orElseThrow();
    }
}
