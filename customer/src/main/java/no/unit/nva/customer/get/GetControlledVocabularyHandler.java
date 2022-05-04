package no.unit.nva.customer.get;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import java.util.UUID;
import no.unit.nva.customer.ControlledVocabularyHandler;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.VocabularyList;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class GetControlledVocabularyHandler extends ControlledVocabularyHandler<Void, VocabularyList> {

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(customerService,Void.class);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularyList output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected VocabularyList processInput(Void input, RequestInfo requestInfo, Context context)
        throws NotFoundException {
        UUID identifier = extractIdentifier(requestInfo);
        CustomerDto customerDto = customerService.getCustomer(identifier);
        return VocabularyList.fromCustomerDto(customerDto);
    }
}
