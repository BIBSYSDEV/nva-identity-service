package no.unit.nva.customer.get;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.customer.model.LinkedDataContextUtils.toId;
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
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;

public class GetCustomerDoiHandler extends CustomerDoiHandler<Void> {

    public static final String DOI_AGENT = "doiagent";
    private final SecretsReader secretsReader;

    /**
     * Default Constructor for GetCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public GetCustomerDoiHandler() {
        this(new SecretsReader());
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param secretsReader A valid SecretsReader.
     */
    public GetCustomerDoiHandler(SecretsReader secretsReader) {
        super(Void.class);
        this.secretsReader = secretsReader;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected DoiAgentDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        authorizeDoiAgentRead(requestInfo);

        var identifier = getIdentifier(requestInfo);
        var secret = getSecretsManagerDoiAgent().get(identifier);
        var doiAgentId =
            UriWrapper.fromUri(toId(identifier))
                .addChild(DOI_AGENT)
                .getUri();

        // if you want to show the password, add it here...
        return new DoiAgentDto(secret)
                   .addId(doiAgentId);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, DoiAgentDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    private Map<UUID, SecretManagerDoiAgentDao> getSecretsManagerDoiAgent() throws NotFoundException {
        try {
            var secretAsStringJsonArray = secretsReader.fetchSecret(
                CUSTOMER_DOI_AGENT_SECRETS_NAME,
                CUSTOMER_DOI_AGENT_SECRETS_NAME);

            return Arrays.stream(dtoObjectMapper.readValue(secretAsStringJsonArray, SecretManagerDoiAgentDao[].class))
                       .collect(Collectors.toMap(it -> toUuid(it.getCustomerId()), it -> it));
        } catch (Exception ex) {
            throw new NotFoundException(ex.getMessage());
        }
    }

    private UUID toUuid(URI customerId) {
        return
            UUID.fromString(
                UriWrapper.fromUri(customerId).getLastPathElement()
            );
    }
}
