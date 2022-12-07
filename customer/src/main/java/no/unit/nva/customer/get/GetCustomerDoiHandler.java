package no.unit.nva.customer.get;

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
import nva.commons.secrets.SecretsReader;

public class GetCustomerDoiHandler extends CustomerDoiHandler<DoiAgentDto> {

    private final CustomerService customerService;

    private final SecretsReader secretsReader;

    /**
     * Default Constructor for GetCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public GetCustomerDoiHandler() {
        this(defaultCustomerService(), new SecretsReader());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param customerService customerService
     * @param secretsReader a vaild SecretsReader
     */
    public GetCustomerDoiHandler(CustomerService customerService, SecretsReader secretsReader) {
        super(DoiAgentDto.class);
        this.customerService = customerService;
        this.secretsReader = secretsReader;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected DoiAgentDto processInput(DoiAgentDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        // TODO Implement access control ?  -->  authorizeDoiAgentChange(requestInfo);
        var identifier = getIdentifier(requestInfo);
        var doiAgent = customerService.getCustomer(identifier).getDoiAgent();
        var secret = secretsReader.fetchSecret(CUSTOMER_DOI_AGENT_SECRETS_NAME, identifier.toString());
        return doiAgent.addSecret(secret);
    }

    @Override
    protected Integer getSuccessStatusCode(DoiAgentDto input, DoiAgentDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
