package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.TermsConditionsResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

import java.nio.file.Path;

import static nva.commons.core.attempt.Try.attempt;

public class GetCurrentTermsConditionsHandler extends ApiGatewayHandler<Void, TermsConditionsResponse> {

    private final IdentityService databaseService;

    @JacocoGenerated
    public GetCurrentTermsConditionsHandler() {
        this(
            IdentityService.defaultIdentityService()
        );
    }

    public GetCurrentTermsConditionsHandler(IdentityService databaseService) {
        super(Void.class);
        this.databaseService = databaseService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        // do nothing for now...
    }

    @Override
    protected TermsConditionsResponse processInput(Void unused, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var json = IoUtils.stringFromResources(Path.of("currentTermsConditions.json"));
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, TermsConditionsResponse.class)).orElseThrow();

    }

    @Override
    protected Integer getSuccessStatusCode(Void unused, TermsConditionsResponse o) {
        return 200;
    }
}
