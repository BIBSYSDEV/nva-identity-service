package no.unit.nva.customer.get;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerDoiHandler;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import no.unit.nva.customer.model.SecretManagerDoiAgentDao;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.secrets.SecretsReader;

import java.net.HttpURLConnection;
import java.util.List;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public class GetCustomerDoiHandler extends CustomerDoiHandler<Void> {

    /**
     * Default Constructor for ListCustomerHandler.
     */
    @JacocoGenerated
    @SuppressWarnings("unused")
    public GetCustomerDoiHandler() {
        super(Void.class);
    }

    /**
     * Constructor for CreateCustomerHandler.
     *
     * @param secretsReader A valid SecretsReader.
     */
    public GetCustomerDoiHandler(SecretsReader secretsReader) {
        super(Void.class, secretsReader);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        authorizeDoiAgentRead(requestInfo);
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {


        var identifier = getIdentifier(requestInfo);

        var doiSecretDao = attempt(() -> getSecretsManagerDoiAgent().get(identifier))
                .orElseThrow(this::throwException);

        var doiAgentDto = nonNull(doiSecretDao)
                ? doiSecretDao.toDoiAgentDto()
                : new DoiAgentDto().addIdByIdentifier(identifier);

        return attempt(() -> mapperToJsonCompact.writeValueAsString(doiAgentDto)).orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    private RuntimeException throwException(Failure<SecretManagerDoiAgentDao> failure) {
        var message = attempt(() -> failure.getException().getMessage()).get();
        return new IllegalArgumentException(message);
    }
}
