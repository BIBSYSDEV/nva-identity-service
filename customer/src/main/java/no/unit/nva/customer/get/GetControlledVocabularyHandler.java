package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class GetControlledVocabularyHandler extends ApiGatewayHandler<Void, VocabularySettingsList> {

    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";
    private static final List<MediaType> SUPPORTED_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8,MediaTypes.APPLICATION_JSON_LD);
    private final CustomerService customerService;

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected VocabularySettingsList processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = UUID.fromString(requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER));
        Set<VocabularySettingDto> vocabularySettings = customerService.getCustomer(identifier).
            getVocabularySettings();
        return new VocabularySettingsList(vocabularySettings);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }
}
