package no.unit.nva.handlers.authorizer;

import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

/**
 * Abstract class for implementing a Simple Request Authorizer. Implementation is based on the AWS examples found in the
 * following page : "https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-use-lambda-authorizer
 * .html".
 */

public abstract class SimpleRequestAuthorizer extends RestRequestHandler<Void, SimpleAuthorizerResponse> {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Logger logger = LoggerFactory.getLogger(SimpleRequestAuthorizer.class);

    public SimpleRequestAuthorizer(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected SimpleAuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {

        logger.debug("Requesting authorizing: " + principalId());
        secretCheck(requestInfo);

        SimpleAuthorizerResponse response = new SimpleAuthorizerResponse();
        response.setIsAuthorized(true);

        return response;
    }

    @Override
    @JacocoGenerated
    protected Integer getSuccessStatusCode(Void input, SimpleAuthorizerResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected void writeOutput(Void input, SimpleAuthorizerResponse output, RequestInfo requestInfo)
            throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            String responseJson = dtoObjectMapper.writeValueAsString(output);
            writer.write(responseJson);
        }
    }

    @Override
    @JacocoGenerated
    protected void writeExpectedFailure(Void input, ApiGatewayException exception, String requestId)
            throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    @Override
    @JacocoGenerated
    protected void writeUnexpectedFailure(Void input, Exception exception, String requestId)
            throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    protected abstract String principalId() throws ForbiddenException;

    protected abstract String fetchSecret() throws ForbiddenException;

    protected void secretCheck(RequestInfo requestInfo) throws ForbiddenException {
        Optional.ofNullable(requestInfo.getHeaders().get(AUTHORIZATION_HEADER))
                .map(this::validateSecret)
                .filter(this::validationSucceeded)
                .orElseThrow(ForbiddenException::new);
    }

    private Boolean validationSucceeded(Boolean check) {
        return check;
    }

    private boolean validateSecret(String clientSecret) {
        String correctSecret = attempt(this::fetchSecret).orElseThrow(this::logErrorAndThrowException);
        return clientSecret.equals(correctSecret);
    }

    private void writeFailure() throws IOException, ForbiddenException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            SimpleAuthorizerResponse denyResponse = new SimpleAuthorizerResponse();
            denyResponse.setIsAuthorized(false);
            String response = dtoObjectMapper.writeValueAsString(denyResponse);
            writer.write(response);
        }
    }

    private RuntimeException logErrorAndThrowException(Failure<String> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new RuntimeException(failure.getException());
    }
}
