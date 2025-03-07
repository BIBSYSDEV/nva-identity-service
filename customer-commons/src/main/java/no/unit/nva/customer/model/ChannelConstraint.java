package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum ChannelConstraint {
    @JsonProperty("OwnerOnlyPublishes")
    OWNER_ONLY_PUBLISHES("OwnerOnlyPublishes"),
    @JsonProperty("EveryonePublishes")
    EVERYONE_PUBLISHES("EveryonePublishes"),
    @JsonProperty("OwnerOnlyEdits")
    OWNER_ONLY_EDITS("OwnerOnlyEdits"),
    @JsonProperty("EveryoneEdits")
    EVERYONE_EDITS("EveryoneEdits");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ChannelConstraint, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;

    ChannelConstraint(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ChannelConstraint lookUp(String value) {
        return stream(values())
                .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                .collect(SingletonCollector.tryCollect())
                .orElseThrow(failure -> throwException(failure, value));
    }

    private static RuntimeException throwException(Failure<ChannelConstraint> failure, String value) {
        return new IllegalArgumentException(
                format(ERROR_MESSAGE_TEMPLATE, value, stream(ChannelConstraint.values())
                        .map(ChannelConstraint::toString).collect(joining(DELIMITER))));
    }
}
