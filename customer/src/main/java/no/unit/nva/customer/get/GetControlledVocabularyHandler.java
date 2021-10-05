package no.unit.nva.customer.get;

import static no.unit.nva.customer.api.constants.Constants.IDENTIFIER_PATH_PARAMETER;
import static no.unit.nva.customer.api.constants.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class GetControlledVocabularyHandler extends ControlledVocabularyHandler<Void, VocabularySettingsList> {

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(Void.class, customerService);
    }

    @Override
    protected VocabularySettingsList processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = UUID.fromString(requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER));
        Set<VocabularySettingDto> vocabularySettings =
            customerService.getCustomer(identifier).getVocabularySettings();
        return new VocabularySettingsList(vocabularySettings);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_OK;
    }
}
