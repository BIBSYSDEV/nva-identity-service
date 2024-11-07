package no.unit.nva.customer.model.dynamo.converters;

import nva.commons.core.JacocoGenerated;

import java.util.Collection;

import static java.util.Objects.nonNull;

public final class DynamoUtils {

    @JacocoGenerated
    private DynamoUtils() {

    }

    public static boolean nonEmpty(Collection<?> collection) {
        return nonNull(collection) && !collection.isEmpty();
    }
}
