package no.unit.nva.customer;

import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfo;

public abstract class ControlledVocabularyHandler<I, O> extends ApiGatewayHandler<I, O> {

    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";

    public static final List<MediaType> SUPPORTED_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
    protected final CustomerService customerService;

    public ControlledVocabularyHandler(Class<I> iclass, CustomerService customerService) {
        super(iclass);

        this.customerService = customerService;
    }

    @Override
    protected final List<MediaType> listSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }


    protected static UUID extractIdentifier(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER))
            .map(UUID::fromString)
            .orElseThrow();
    }
}