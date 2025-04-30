package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RequestUtils.getChannelClaimIdentifier;
import static no.unit.nva.customer.RequestUtils.getIdentifier;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class DeleteChannelClaimHandler extends ApiGatewayHandler<Void, Void> {

    private final CustomerService customerService;

    @JacocoGenerated
    public DeleteChannelClaimHandler() {
        this(defaultCustomerService(), new Environment());
    }

    public DeleteChannelClaimHandler(CustomerService customerService, Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        if (!isAuthorizedToUnclaimChannel(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        customerService.deleteChannelClaim(getChannelClaimIdentifier(requestInfo));

        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_NO_CONTENT;
    }

    private static boolean isAuthorizedToUnclaimChannel(RequestInfo requestInfo)
        throws BadRequestException, UnauthorizedException {
        var customerIdentifierFromPath = getIdentifier(requestInfo).toString();
        var customerIdentifierFromUser = UriWrapper.fromUri(requestInfo.getCurrentCustomer()).getLastPathElement();
        return customerIdentifierFromUser.equals(customerIdentifierFromPath) &&
               requestInfo.getAccessRights().contains(AccessRight.MANAGE_CHANNEL_CLAIMS);
    }
}
