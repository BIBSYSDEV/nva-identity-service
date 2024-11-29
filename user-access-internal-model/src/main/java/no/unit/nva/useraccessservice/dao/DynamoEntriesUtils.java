package no.unit.nva.useraccessservice.dao;

import java.util.Collection;

import static java.util.Objects.nonNull;

public final class DynamoEntriesUtils {

    private DynamoEntriesUtils() {

    }

    public static boolean nonEmpty(Collection<?> set) {
        return nonNull(set) && !set.isEmpty();
    }
}
