package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetCurrentTermsConditionsHandler extends ApiGatewayHandler<Void, TermsConditionsResponse> {

    @JacocoGenerated
    public GetCurrentTermsConditionsHandler() {
        this(new Environment());
    }

    public GetCurrentTermsConditionsHandler(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        // do nothing for now...
    }

    @Override
    protected TermsConditionsResponse processInput(Void unused, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var termsConditionsUri = new TermsAndConditionsService()
            .getCurrentTermsAndConditions().termsConditionsUri();

        return TermsConditionsResponse.builder()
            .withTermsConditionsUri(termsConditionsUri)
            .build();
    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }
}
