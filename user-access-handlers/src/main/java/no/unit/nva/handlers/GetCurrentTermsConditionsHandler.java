package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;

public class GetCurrentTermsConditionsHandler extends ApiGatewayHandler<Void, TermsConditionsResponse> {

    private static final String TERMS_URL = "https://nva.sikt.no/terms/2024-10-01";
    private static final URI ID = URI.create(TERMS_URL);
    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2024, 10, 1, 0, 0, 0);

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
                .withId(ID)
                .withValidFrom(VALID_FROM)
                .build();

    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }
}
