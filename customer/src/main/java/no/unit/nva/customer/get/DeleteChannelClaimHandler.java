package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class DeleteChannelClaimHandler extends ApiGatewayHandler<Void, Void> {

    protected static final String INVALID_CHANNEL_CLAIM_MESSAGE = "Channel claim identifier is missing or invalid!";
    protected static final String IDENTIFIER_PATH_PARAM = "identifier";
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
        if (!requestInfo.getAccessRights().contains(AccessRight.MANAGE_CHANNEL_CLAIMS)) {
            throw new ForbiddenException();
        }
    }

    @Override
    protected Void processInput(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        customerService.deleteChannelClaim(getIdentifier(requestInfo));

        return null;
    }

    private static UUID getIdentifier(RequestInfo requestInfo) throws BadRequestException {
        return attempt(() -> requestInfo.getPathParameter(IDENTIFIER_PATH_PARAM))
                   .map(UUID::fromString)
                   .orElseThrow(failure -> new BadRequestException(INVALID_CHANNEL_CLAIM_MESSAGE));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_NO_CONTENT;
    }
}
