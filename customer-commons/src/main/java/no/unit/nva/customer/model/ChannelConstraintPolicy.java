package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum ChannelConstraintPolicy {
    OWNER_ONLY("OwnerOnly"),
    EVERYONE("Everyone");

    private static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ChannelConstraintPolicy, expected one of: %s";
    private static final String DELIMITER = ", ";

    private final String value;

    ChannelConstraintPolicy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ChannelConstraintPolicy lookUp(String value) {
        return stream(values())
                .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                .collect(SingletonCollector.tryCollect())
                .orElseThrow(failure -> throwException(value));
    }

    private static RuntimeException throwException(String value) {
        return new IllegalArgumentException(
                format(ERROR_MESSAGE_TEMPLATE, value, stream(values())
                        .map(ChannelConstraintPolicy::toString).collect(joining(DELIMITER))));
    }
}
