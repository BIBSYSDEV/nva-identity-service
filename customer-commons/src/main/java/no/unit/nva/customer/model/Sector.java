package no.unit.nva.customer.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

public enum Sector {
    @JsonProperty("Universitet og høyskoler")
    UHI("Universitet og høyskoler"),
    @JsonProperty("Helsesektoren")
    HELSE("Helsesektoren"),
    @JsonProperty("Instituttsektoren")
    INSTITUTT("Instituttsektoren");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid Sector, expected one of: %s";
    public static final String DELIMITER = ", ";
    private final String value;

    Sector(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Sector lookUp(String value) {
        return stream(values())
                   .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(failure -> throwException(value));
    }

    private static RuntimeException throwException(String value) {
        return new IllegalArgumentException(
            format(ERROR_MESSAGE_TEMPLATE,
                   value,
                   stream(Sector.values()).map(Sector::toString).collect(joining(DELIMITER))));
    }
}
