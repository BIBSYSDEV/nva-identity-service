package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;

public class UpdateCustomerDoiHandler extends CustomerDoiHandler<DoiAgentDto> {

    private final CustomerService customerService;

    /**
     * Default Constructor for UpdateCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public UpdateCustomerDoiHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for UpdateCustomerHandler.
     *
     * @param customerService customerService
     */
    public UpdateCustomerDoiHandler(CustomerService customerService) {
        super(DoiAgentDto.class);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected String processInput(DoiAgentDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = getIdentifier(requestInfo);
        return customerService.updateCustomerDoiAgentSecret(identifier, input).toString();
    }

    @Override
    protected Integer getSuccessStatusCode(DoiAgentDto input, String output) {
        return null;
    }
}
