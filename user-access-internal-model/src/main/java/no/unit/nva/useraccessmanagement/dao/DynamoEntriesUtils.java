package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import java.util.Collection;

public final class DynamoEntriesUtils {

    private DynamoEntriesUtils() {

    }

    public static boolean nonEmpty(Collection<?> set) {
        return nonNull(set) && !set.isEmpty();
    }
}
