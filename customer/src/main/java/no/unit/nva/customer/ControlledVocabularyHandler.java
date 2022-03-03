package no.unit.nva.customer;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.MediaTypes;

public abstract class ControlledVocabularyHandler<I, O> extends ApiGatewayHandlerV2<I, O> {

    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";

    public static final List<MediaType> SUPPORTED_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
    protected final CustomerService customerService;

    public ControlledVocabularyHandler(CustomerService customerService) {
        super();
        this.customerService = customerService;
    }

    protected static UUID extractIdentifier(APIGatewayProxyRequestEvent requestInfo) {
        return attempt(() -> requestInfo.getPathParameters().get(IDENTIFIER_PATH_PARAMETER))
            .map(UUID::fromString)
            .orElseThrow();
    }

    @Override
    protected final List<MediaType> listSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }
}
