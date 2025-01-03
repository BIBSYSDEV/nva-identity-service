package no.unit.nva.customer.model;

import nva.commons.core.JacocoGenerated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;

@JacocoGenerated
public final class JacksonJrDoesNotSupportSets {

    private JacksonJrDoesNotSupportSets() {

    }

    public static <T> List<T> toList(Collection<T> collection) {
        return nonNull(collection) ? new ArrayList<>(collection) : Collections.emptyList();
    }

    public static <T> Set<T> toSet(Collection<T> collection) {
        return nonNull(collection) ? new HashSet<>(collection) : Collections.emptySet();
    }
}

