package no.unit.nva.database;

import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.EmptyInputException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.interfaces.Validable;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

public class DatabaseSubService {

    public static final String EMPTY_INPUT_ERROR_MESSAGE = "Expected non-empty input, but input is empty";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSubService.class);
    protected DynamoDbEnhancedClient client;

    protected DatabaseSubService(DynamoDbClient client) {
        this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    protected static void validate(Validable input) throws InvalidInputException {
        if (isNull(input)) {
            throw new EmptyInputException(EMPTY_INPUT_ERROR_MESSAGE);
        }
        if (isInvalid(input)) {
            throw input.exceptionWhenInvalid();
        }
    }

    protected static boolean isInvalid(Validable validable) {
        return isNull(validable) || validable.isInvalid();
    }

    protected static String convertToStringOrWriteErrorMessage(Object queryObject) {
        return Optional.ofNullable(queryObject)
            .map(attempt(JsonConfig::writeValueAsString))
            .map(Try::orElseThrow)
            .orElse(EMPTY_INPUT_ERROR_MESSAGE);
    }

    // PMD complains about the log error format but this call seems legit according to SLF4J
    // see http://slf4j.org/faq.html#exception_message
    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    protected static <T> RuntimeException handleError(Failure<T> fail) {
        logger.error("Error fetching user:", fail.getException());
        if (fail.getException() instanceof RuntimeException) {
            return (RuntimeException) fail.getException();
        } else {
            throw new RuntimeException(fail.getException());
        }
    }
}
