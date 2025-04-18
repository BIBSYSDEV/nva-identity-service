package no.unit.nva.customer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.zalando.problem.jackson.ProblemModule;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.core.attempt.Try.attempt;

public abstract class CustomerDoiHandler<I> extends ApiGatewayHandler<I, String> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    protected static final String SECRETS_KEY_AND_NAME = "dataCiteCustomerSecrets";
    protected final SecretsReader secretsReader;

    protected final ObjectMapper mapperToJsonCompact = createJsonParser();
    // dtoObjectMapper.setSerializationInclusion(Include.ALWAYS);

    @JacocoGenerated
    public CustomerDoiHandler(Class<I> iclass) {
        this(iclass, new SecretsReader(), new Environment());
    }

    public CustomerDoiHandler(Class<I> iclass, SecretsReader secretsReader, Environment environment) {
        super(iclass, environment);
        this.secretsReader = secretsReader;
    }

    protected UUID getIdentifier(RequestInfo requestInfo) throws InputException {
        String identifier = RequestUtils.getPathParameter(requestInfo, IDENTIFIER).orElse(null);
        return attempt(() -> UUID.fromString(identifier))
            .orElseThrow(fail -> handleIdentifierParsingError(identifier, fail));
    }

    private InputException handleIdentifierParsingError(String identifier, Failure<UUID> fail) {
        return new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, fail.getException());
    }

    protected Map<UUID, SecretManagerDoiAgentDao> getSecretsManagerDoiAgent() throws JsonProcessingException {

        var secretAsStringJsonArray = secretsReader.fetchSecret(SECRETS_KEY_AND_NAME, SECRETS_KEY_AND_NAME);

        if (isNull(secretAsStringJsonArray)) {
            secretAsStringJsonArray = "[]";
        }

        return Arrays.stream(dtoObjectMapper.readValue(secretAsStringJsonArray, SecretManagerDoiAgentDao[].class))
            .collect(Collectors.toConcurrentMap(
                this::extractKey,
                doiAgent -> doiAgent,
                SecretManagerDoiAgentDao::merge));
    }

    private UUID extractKey(SecretManagerDoiAgentDao doiAgent) {
        return toUuid(doiAgent.getCustomerId());
    }

    private UUID toUuid(URI customerId) {
        return
            UUID.fromString(
                UriWrapper.fromUri(customerId).getLastPathElement()
            );
    }

    protected void authorizeDoiAgentRead(RequestInfo requestInfo) throws ForbiddenException {
        if (notCustomerManager(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean notCustomerManager(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(MANAGE_CUSTOMERS);
    }

    private ObjectMapper createJsonParser() {
        var jsonFactory =
            new JsonFactory()
                .configure(Feature.ALLOW_SINGLE_QUOTES, true);
        return
            new ObjectMapper(jsonFactory)
                .registerModule(new ProblemModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(Include.ALWAYS);
    }

}
