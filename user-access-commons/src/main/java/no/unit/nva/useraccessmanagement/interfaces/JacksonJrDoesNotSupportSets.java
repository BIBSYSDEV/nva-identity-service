package no.unit.nva.useraccessmanagement.interfaces;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nva.commons.core.JacocoGenerated;

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
