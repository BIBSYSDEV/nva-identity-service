package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import nva.commons.secrets.SecretsWriter;

public class UpdateCustomerDoiHandler extends CustomerDoiHandler<String> {

    private final CustomerService customerService;
    private final SecretsWriter secretsWriter;

    /**
     * Default Constructor for UpdateCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public UpdateCustomerDoiHandler() {
        this(defaultCustomerService(), new SecretsWriter(), new SecretsReader());
    }

    /**
     * Constructor for UpdateCustomerHandler.
     *
     * @param customerService customerService
     * @param secretsWriter   a SecretsWriter
     * @param secretsReader   a SecretsReader
     */
    public UpdateCustomerDoiHandler(CustomerService customerService, SecretsWriter secretsWriter,
                                    SecretsReader secretsReader) {
        super(String.class, secretsReader);
        this.customerService = customerService;
        this.secretsWriter = secretsWriter;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected String processInput(String input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        authorizeDoiAgentChange(requestInfo);

        var customerId = getIdentifier(requestInfo);

        var persistedDoiAgent = updateSecretManager(customerId, input);

        updateCustomer(customerId, persistedDoiAgent);

        return attempt(() -> mapperToJsonCompact.writeValueAsString(persistedDoiAgent)).orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(String input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void updateCustomer(UUID customerId, DoiAgentDto input)
        throws NotFoundException, InputException {

        var customer = customerService.getCustomer(customerId);
        customer.setDoiAgent(input);

        customerService.updateCustomer(customerId, customer);
    }

    private DoiAgentDto updateSecretManager(UUID customerId, String input)
        throws BadRequestException {

        var allSecrets = attempt(this::getSecretsManagerDoiAgent).orElseThrow();
        var inputSecret = DoiAgentDto.fromJson(input).addIdByIdentifier(customerId);

        if (allSecrets.containsKey(customerId)) {
            allSecrets.get(customerId).merge(inputSecret);
        } else {
            allSecrets.put(customerId, new SecretManagerDoiAgentDao(inputSecret));
        }

        var allSecretsJsonString =
            allSecrets.values().stream()
                .map(SecretManagerDoiAgentDao::toString)
                .collect(Collectors.joining(",", "[", "]"));
        secretsWriter.updateSecretKey(SECRETS_KEY_AND_NAME, SECRETS_KEY_AND_NAME, allSecretsJsonString);

        return allSecrets.get(customerId).toDoiAgentDto();
    }
}
