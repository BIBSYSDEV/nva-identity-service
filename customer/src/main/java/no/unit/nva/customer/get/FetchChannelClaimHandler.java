package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.UUID;
import no.unit.nva.customer.get.response.ChannelClaimResponse;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class FetchChannelClaimHandler extends ApiGatewayHandler<Void, ChannelClaimResponse> {

    private static final String CLAIM_NOT_FOUND = "Channel claim with identifier %s not found";
    private static final String NOT_VALID_IDENTIFIER = "Provided channel claim identifier is not valid!";
    private static final String IDENTIFIER_PATH = "identifier";
    private final CustomerService customerService;

    @JacocoGenerated
    public FetchChannelClaimHandler() {
        this(defaultCustomerService(), new Environment());
    }

    public FetchChannelClaimHandler(CustomerService customerService, Environment environment) {
        super(Void.class, environment);
        this.customerService = customerService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
    }

    @Override
    protected ChannelClaimResponse processInput(Void unused, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var identifier = getIdentifier(requestInfo);
        return customerService.getChannelClaim(identifier)
                   .map(ChannelClaimResponse::create)
                   .orElseThrow(() -> new NotFoundException(CLAIM_NOT_FOUND.formatted(identifier)));
    }

    private static UUID getIdentifier(RequestInfo requestInfo) throws BadRequestException {
        return attempt(() -> requestInfo.getPathParameter(IDENTIFIER_PATH))
                   .map(UUID::fromString)
                   .orElseThrow(failure -> new BadRequestException(NOT_VALID_IDENTIFIER));
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, ChannelClaimResponse o) {
        return HTTP_OK;
    }
}
