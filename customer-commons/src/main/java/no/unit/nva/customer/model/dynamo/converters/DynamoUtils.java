package no.unit.nva.customer.model.dynamo.converters;

import static java.util.Objects.nonNull;
import java.util.Collection;

public final class DynamoUtils {

    private DynamoUtils() {

    }

    public static boolean nonEmpty(Collection<?> collection) {
        return nonNull(collection) && !collection.isEmpty();
    }
}
