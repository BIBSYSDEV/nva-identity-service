package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import no.unit.nva.customer.get.response.ChannelClaimResponse;
import no.unit.nva.customer.get.response.ChannelClaimsListResponse;
import no.unit.nva.customer.get.response.CustomerResponse;
import no.unit.nva.customer.model.ChannelClaimDto;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class ListAllChannelClaimsHandler extends ApiGatewayHandler<Void, ChannelClaimsListResponse> {

    private static final String BAD_GATEWAY_ERROR_MESSAGE = "Something went wrong, contact application administrator!";
    private final CustomerService customerService;

    @JacocoGenerated
    public ListAllChannelClaimsHandler() {
        this(defaultCustomerService());
    }

    public ListAllChannelClaimsHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        userIsAuthorized(requestInfo);
    }

    @Override
    protected ChannelClaimsListResponse processInput(Void unused, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            return ChannelClaimsListResponse.fromChannelClaims(customerService.getChannelClaims());
        } catch (Exception e) {
            throw new BadGatewayException(BAD_GATEWAY_ERROR_MESSAGE);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, ChannelClaimsListResponse o) {
        return HTTP_OK;
    }

    private static void userIsAuthorized(RequestInfo requestInfo) throws UnauthorizedException {
        requestInfo.getCurrentCustomer();
    }
}
