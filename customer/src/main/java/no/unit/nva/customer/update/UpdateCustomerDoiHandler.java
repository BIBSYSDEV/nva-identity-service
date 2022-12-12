package no.unit.nva.customer.update;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import nva.commons.secrets.SecretsWriter;

public class UpdateCustomerDoiHandler extends CustomerDoiHandler<DoiAgentDto> {

    private final CustomerService customerService;
    private final SecretsWriter secretsWriter;
    private final SecretsReader secretsReader;

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
     * @param secretsWriter a SecretsWriter
     * @param secretsReader a SecretsReader
     */
    public UpdateCustomerDoiHandler(CustomerService customerService, SecretsWriter secretsWriter,
                                    SecretsReader secretsReader) {
        super(DoiAgentDto.class);
        this.customerService = customerService;
        this.secretsWriter = secretsWriter;
        this.secretsReader = secretsReader;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected DoiAgentDto processInput(DoiAgentDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var customerId = getIdentifier(requestInfo);

        // TODO Implement access control ?  -->  authorizeDoiAgentChange(requestInfo);

        var customer = customerService.getCustomer(customerId);
        customer.setDoiAgent(input);
        var result = customerService.updateCustomer(customerId, customer);

        var secretsListFiltered = getSecretManagerDoiAgentsFiltered(customer.getId());

        var doiSecret = new SecretManagerDoiAgentDao(customer.getId(), input);

        var allSecretsJsonString =
            Stream.concat(secretsListFiltered, Stream.of(doiSecret))
                .map(SecretManagerDoiAgentDao::toString)
                .collect(Collectors.joining(",", "[", "]"));

        secretsWriter.updateSecretKey(CUSTOMER_DOI_AGENT_SECRETS_NAME, CUSTOMER_DOI_AGENT_SECRETS_NAME,
                                      allSecretsJsonString);
        return result.getDoiAgent()
                   .addPassword(input.getPassword());
    }

    @Override
    protected Integer getSuccessStatusCode(DoiAgentDto input, DoiAgentDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private Stream<SecretManagerDoiAgentDao> getSecretManagerDoiAgentsFiltered(URI customerId) {
        var secretAsStringJsonArray =
            secretsReader.fetchSecret(CUSTOMER_DOI_AGENT_SECRETS_NAME, CUSTOMER_DOI_AGENT_SECRETS_NAME);

        return
            attempt(() -> Arrays.stream(
                dtoObjectMapper.readValue(secretAsStringJsonArray, SecretManagerDoiAgentDao[].class)
            ).filter(p -> !p.getCustomerId().equals(customerId))).orElseThrow();
    }

}
