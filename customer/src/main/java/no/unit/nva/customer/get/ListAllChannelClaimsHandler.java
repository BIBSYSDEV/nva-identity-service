package no.unit.nva.customer.get;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import no.unit.nva.customer.get.response.ChannelClaimsListResponse;
import no.unit.nva.customer.model.channelclaim.ChannelClaimWithClaimer;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListAllChannelClaimsHandler extends ApiGatewayHandler<Void, ChannelClaimsListResponse> {

    private static final String BAD_GATEWAY_ERROR_MESSAGE = "Something went wrong, contact application administrator!";
    private static final String INSTITUTION_QUERY_PARAM = "institution";
    private final CustomerService customerService;

    @JacocoGenerated
    public ListAllChannelClaimsHandler() {
        this(defaultCustomerService(), new Environment());
    }

    public ListAllChannelClaimsHandler(CustomerService customerService, Environment environment) {
        super(Void.class, environment);
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
            return ChannelClaimsListResponse.fromChannelClaims(listChannelClaims(requestInfo));
        } catch (Exception e) {
            throw new BadGatewayException(BAD_GATEWAY_ERROR_MESSAGE);
        }
    }

    private Collection<ChannelClaimWithClaimer> listChannelClaims(RequestInfo requestInfo) {
        return getInstitutionCristinId(requestInfo)
                   .map(customerService::getChannelClaimsForCustomer)
                   .orElse(customerService.getChannelClaims());
    }

    private static Optional<URI> getInstitutionCristinId(RequestInfo requestInfo) {

        return requestInfo.getQueryParameterOpt(INSTITUTION_QUERY_PARAM)
                   .map(ListAllChannelClaimsHandler::decode)
                   .map(URI::create);
    }

    private static String decode(String cristinId) {
        return URLDecoder.decode(cristinId, StandardCharsets.UTF_8);
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, ChannelClaimsListResponse o) {
        return HTTP_OK;
    }

    private static void userIsAuthorized(RequestInfo requestInfo) throws UnauthorizedException {
        requestInfo.getCurrentCustomer();
    }
}
