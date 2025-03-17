package no.unit.nva.customer;

import com.google.common.net.MediaType;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nva.commons.core.attempt.Try.attempt;

public abstract class ControlledVocabularyHandler<I, O> extends ApiGatewayHandler<I, O> {

    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";

    public static final List<MediaType> SUPPORTED_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
    protected final CustomerService customerService;

    public ControlledVocabularyHandler(CustomerService customerService, Class<I> inputClass) {
        super(inputClass);
        this.customerService = customerService;
    }

    protected static UUID extractIdentifier(RequestInfo requestInfo) {
        return attempt(() -> RequestUtils.getPathParameter(requestInfo, IDENTIFIER_PATH_PARAMETER))
            .map(Optional::orElseThrow)
            .map(UUID::fromString)
            .orElseThrow();
    }

    @Override
    protected final List<MediaType> listSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }
}
