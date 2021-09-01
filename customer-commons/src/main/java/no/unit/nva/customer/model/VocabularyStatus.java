package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonValue;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum VocabularyStatus {
    DEFAULT("Default"),
    ALLOWED("Allowed"),
    DISABLED("Disabled");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid VocabularyStatus, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;

    VocabularyStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Lookup enum by value.
     *
     * @param value value
     * @return enum
     */
    public static VocabularyStatus lookup(String value) {
        return stream(values())
                .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        format(ERROR_MESSAGE_TEMPLATE, value, stream(VocabularyStatus.values())
                                .map(VocabularyStatus::toString).collect(joining(DELIMITER)))));
    }

}
