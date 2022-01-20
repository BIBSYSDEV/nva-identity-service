package no.unit.nva.database;

import static java.util.Objects.isNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.Optional;
import no.unit.nva.useraccessmanagement.dao.DynamoEntryWithRangeKey;
import no.unit.nva.useraccessmanagement.exceptions.EmptyInputException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.interfaces.Validable;
import nva.commons.core.JsonSerializable;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseSubService {

    public static final String EMPTY_INPUT_ERROR_MESSAGE = "Expected non-empty input, but input is empty";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSubService.class);

    protected Table table;

    protected DatabaseSubService(Table table) {
        this.table = table;
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

    protected static String convertToStringOrWriteErrorMessage(JsonSerializable queryObject) {
        return Optional.ofNullable(queryObject).map(JsonSerializable::toString).orElse(EMPTY_INPUT_ERROR_MESSAGE);
    }

    protected static Item fetchItemForTable(Table table, DynamoEntryWithRangeKey requestEntry) {
        return table.getItem(
            PRIMARY_KEY_HASH_KEY, requestEntry.getPrimaryHashKey(),
            PRIMARY_KEY_RANGE_KEY, requestEntry.getPrimaryRangeKey()
        );
    }

    // PMD complains about the log error format but this call seems legit according to SLF4J
    // see http://slf4j.org/faq.html#exception_message
    @SuppressWarnings("PMD.InvalidLogMessageFormat")
    protected static <T> InvalidEntryInternalException handleError(Failure<T> fail) {
        logger.error("Error fetching user:", fail.getException());
        if (fail.getException() instanceof InvalidEntryInternalException) {
            return (InvalidEntryInternalException) fail.getException();
        } else {
            throw new RuntimeException(fail.getException());
        }
    }

    protected Item fetchItem(DynamoEntryWithRangeKey requestEntry) {
        return fetchItemForTable(table, requestEntry);
    }
}
