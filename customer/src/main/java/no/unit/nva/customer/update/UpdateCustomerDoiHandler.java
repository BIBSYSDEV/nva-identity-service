package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsWriter;

public class UpdateCustomerDoiHandler extends CustomerDoiHandler<DoiAgentDto> {

    private final CustomerService customerService;
    private final SecretsWriter secretsWriter;

    /**
     * Default Constructor for UpdateCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public UpdateCustomerDoiHandler() {
        this(defaultCustomerService(), new SecretsWriter());
    }

    /**
     * Constructor for UpdateCustomerHandler.
     *
     * @param customerService customerService
     */
    public UpdateCustomerDoiHandler(CustomerService customerService, SecretsWriter secretsWriter) {
        super(DoiAgentDto.class);
        this.customerService = customerService;
        this.secretsWriter = secretsWriter;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected DoiAgentDto processInput(DoiAgentDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var customerId = getIdentifier(requestInfo);
        var customer = customerService.getCustomer(customerId);
        // TODO Implement access control ?  -->  authorizeDoiAgentChange(requestInfo);
        customer.setDoiAgent(input);
        secretsWriter.updateSecretKey(CUSTOMER_DOI_AGENT_SECRETS_NAME, customerId.toString(), input.getSecret());
        var result = customerService.updateCustomer(customerId, customer);
        return result
                   .getDoiAgent()
                   .addSecret(input.getSecret());
    }

    @Override
    protected Integer getSuccessStatusCode(DoiAgentDto input, DoiAgentDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
