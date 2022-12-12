package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class GetCustomerDoiHandler extends CustomerDoiHandler<Void> {

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
     * @param secretsReader   a vaild SecretsReader
     */
    public GetCustomerDoiHandler(CustomerService customerService, SecretsReader secretsReader) {
        super(Void.class);
        this.customerService = customerService;
        this.secretsReader = secretsReader;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected DoiAgentDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        // TODO Implement access control ?  -->  authorizeDoiAgentChange(requestInfo);
        var identifier = getIdentifier(requestInfo);
        var secretMap = getSecretManagerDoiAgent();
        var secret = secretMap.get(identifier);

        return new DoiAgentDto(secret)
                   .addPassword(secret.getPassword());
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiAgentDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private Map<UUID, SecretManagerDoiAgentDao> getSecretManagerDoiAgent() throws NotFoundException {
        try {
            var secretAsStringJsonArray = secretsReader.fetchSecret(CUSTOMER_DOI_AGENT_SECRETS_NAME,
                                                                    CUSTOMER_DOI_AGENT_SECRETS_NAME);

            return Arrays.stream(dtoObjectMapper.readValue(secretAsStringJsonArray, SecretManagerDoiAgentDao[].class))
                       .collect(Collectors.toMap(it -> toUuid(it.getCustomerId()), it -> it));
        } catch (Exception ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }

    private UUID toUuid(URI customerId) {
        var parts = customerId.getPath().split("/");
        return UUID.fromString(parts[parts.length - 1]);
    }
}
