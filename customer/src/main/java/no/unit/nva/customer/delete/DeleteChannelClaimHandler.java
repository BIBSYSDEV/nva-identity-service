package no.unit.nva.customer.delete;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RequestUtils.getChannelClaimIdentifier;
import static no.unit.nva.customer.RequestUtils.getIdentifier;
import static nva.commons.apigateway.AccessRight.MANAGE_CHANNEL_CLAIMS;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

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
        if (!requestInfo.userIsAuthorized(MANAGE_CHANNEL_CLAIMS)) {
            throw new ForbiddenException();
        }
        validateUuids(requestInfo);
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

    private void validateUuids(RequestInfo requestInfo) throws BadRequestException {
        getIdentifier(requestInfo);
        getChannelClaimIdentifier(requestInfo);
    }
}
