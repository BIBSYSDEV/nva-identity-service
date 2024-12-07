package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.database.TermsAndConditionsService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

public class UpdatePersonTermsConditionsHandler extends
    ApiGatewayHandler<TermsConditionsResponse, TermsConditionsResponse> {

    private final TermsAndConditionsService service;

    @JacocoGenerated
    public UpdatePersonTermsConditionsHandler() {
        this(new TermsAndConditionsService());
    }

    public UpdatePersonTermsConditionsHandler(TermsAndConditionsService service) {
        super(TermsConditionsResponse.class);
        this.service = service;
    }


    @Override
    protected void validateRequest(
        TermsConditionsResponse input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        requestInfo.getPersonCristinId();
        requestInfo.getCurrentCustomer();
    }

    @Override
    protected TermsConditionsResponse processInput(
        TermsConditionsResponse input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return service.updateTermsAndConditions(
            requestInfo.getPersonCristinId(),
            input.termsConditionsUri(),
            requestInfo.getUserName()
        );
    }

    @Override
    protected Integer getSuccessStatusCode(
        TermsConditionsResponse termsConditionsResponse, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }
}
