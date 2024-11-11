package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;
import java.net.URI;

public class GetCurrentTermsConditionsHandler extends ApiGatewayHandler<Void, TermsConditionsResponse> {

    private static final String TERMS_URL = "https://nva.sikt.no/terms/2024-10-01";
    private static final URI ID = URI.create(TERMS_URL);

    public GetCurrentTermsConditionsHandler() {
        super(Void.class);
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        // do nothing for now...
    }

    @Override
    protected TermsConditionsResponse processInput(Void unused, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(ID)
                .build();

    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }
}
