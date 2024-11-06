package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.TermsConditions;
import no.unit.nva.customer.service.impl.DynamoCrudService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

import static no.unit.nva.customer.Constants.PERSISTED_ENTITY;

public class UpdatePersonTermsConditionsHandler extends ApiGatewayHandler<TermsConditionsResponse, TermsConditionsResponse> {

    private final DynamoCrudService<TermsConditions> service;

    public UpdatePersonTermsConditionsHandler() {
        super(TermsConditionsResponse.class);
        service = new DynamoCrudService<>(PERSISTED_ENTITY, TermsConditions.class);
    }

    @Override
    protected void validateRequest(TermsConditionsResponse termsConditionsResponse, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        requestInfo.getPersonCristinId();
    }

    @Override
    protected TermsConditionsResponse processInput(TermsConditionsResponse termsConditionsResponse, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var upserted = TermsConditions.builder()
                .withId(requestInfo.getPersonCristinId())
                .termsConditionsUri(termsConditionsResponse.termsConditionsUri())
                .build()
                .upsert(service);

        return TermsConditionsResponse.builder()
                .withTermsConditionsUri(upserted.termsConditionsUri())
                .build();
    }

    @Override
    protected Integer getSuccessStatusCode(TermsConditionsResponse termsConditionsResponse, TermsConditionsResponse o) {
        return HttpURLConnection.HTTP_OK;
    }
}
