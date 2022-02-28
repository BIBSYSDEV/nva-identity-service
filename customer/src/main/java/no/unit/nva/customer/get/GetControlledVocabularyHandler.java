package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.VocabularyListDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class GetControlledVocabularyHandler extends ControlledVocabularyHandler<Void, VocabularyListDto> {

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(Void.class, customerService);
    }

    @Override
    protected VocabularyListDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = UUID.fromString(requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER));
        CustomerDao customerDao = customerService.getCustomer(identifier);
        return VocabularyListDto.fromCustomerDto(customerDao.toCustomerDto());
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularyListDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
