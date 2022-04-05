package no.unit.nva.customer.model;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.customer.model.dynamo.converters.VocabularyStatusConverter;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;

public enum VocabularyStatus {
    //2.14 version of JacksonJr will support @JsonCreator
    @JsonProperty("Default") DEFAULT("Default"),
    @JsonProperty("Allowed")ALLOWED("Allowed"),
    @JsonProperty("Disabled")DISABLED("Disabled");

    public static final AttributeConverter<VocabularyStatus> VOCABULARY_STATUS_CONVERTER =
        new VocabularyStatusConverter();
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

    @JsonCreator
    public static VocabularyStatus lookUp(String value) {
        return  stream(values())
                .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
                .collect(SingletonCollector.tryCollect())
                .orElseThrow(failure -> throwException(failure, value));
    }

    private static RuntimeException throwException(Failure<VocabularyStatus> failure, String value) {
        return new IllegalArgumentException(
                format(ERROR_MESSAGE_TEMPLATE, value, stream(VocabularyStatus.values())
                        .map(VocabularyStatus::toString).collect(joining(DELIMITER))));
    }


}
