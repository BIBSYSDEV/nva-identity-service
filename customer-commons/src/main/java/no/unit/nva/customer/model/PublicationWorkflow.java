package no.unit.nva.customer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public enum PublicationWorkflow {
    @JsonProperty("RegistratorPublishesMetadataOnly")
    REGISTRATOR_PUBLISHES_METADATA_ONLY("RegistratorPublishesMetadataOnly"),
    @JsonProperty("RegistratorPublishesMetadataAndFiles")
    REGISTRATOR_PUBLISHES_METADATA_AND_FILES("RegistratorPublishesMetadataAndFiles"),
    @JsonProperty("RegistratorRequiresApprovalForMetadataAndFiles")
    REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES("RegistratorRequiresApprovalForMetadataAndFiles");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid PublicationWorkflow, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;

    PublicationWorkflow(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PublicationWorkflow lookUp(String value) {
        return stream(values())
            .filter(nameType -> nameType.getValue().equalsIgnoreCase(value))
            .collect(SingletonCollector.tryCollect())
            .orElseThrow(failure -> throwException(failure, value));
    }

    private static RuntimeException throwException(Failure<PublicationWorkflow> failure, String value) {
        return new IllegalArgumentException(
            format(ERROR_MESSAGE_TEMPLATE, value, stream(PublicationWorkflow.values())
                .map(PublicationWorkflow::toString).collect(joining(DELIMITER))));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
