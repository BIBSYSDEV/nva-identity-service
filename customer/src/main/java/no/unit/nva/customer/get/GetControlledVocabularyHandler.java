package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.interfaces.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.JacocoGenerated;

public class GetControlledVocabularyHandler extends ControlledVocabularyHandler<Void, VocabularyList> {

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(customerService);
    }

    @Override
    protected VocabularyList processInput(String  input, APIGatewayProxyRequestEvent requestInfo, Context context){
        UUID identifier = UUID.fromString(requestInfo.getPathParameters().get(IDENTIFIER_PATH_PARAMETER));
        CustomerDto customerDto = customerService.getCustomer(identifier);
        return VocabularyList.fromCustomerDto(customerDto);
    }

    @Override
    protected Integer getSuccessStatusCode(String input, VocabularyList output) {
        return HttpURLConnection.HTTP_OK;
    }
}
