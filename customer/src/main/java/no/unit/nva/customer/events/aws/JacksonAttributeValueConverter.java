package no.unit.nva.customer.events.aws;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class JacksonAttributeValueConverter implements AttributeValueConverter {

    public JacksonAttributeValueConverter() {
    }

    @Override
    public Map<String, AttributeValue> convert(
        final Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> image
    ) {
        return image.entrySet().stream().collect(toDynamoDbMap());
    }

    private static Collector<
                                Entry<
                                         String,
                                         com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue>,
                                ?,
                                Map<String, AttributeValue>>
    toDynamoDbMap() {
        return Collectors.toMap(
            Entry::getKey,
            attributeValue ->
                attempt(() -> mapToDynamoDbValue(attributeValue.getValue())).orElseThrow());
    }

    private static AttributeValue mapToDynamoDbValue(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value)
        throws JsonProcessingException {
        if (isNullValue(value)) {
            return AttributeValue.builder().nul(true).build();
        }
        if (nonNull(value.getL()) && value.getL().isEmpty()) {
            return AttributeValue.builder().l(emptyList()).build();
        }
        var json = writeAsString(value);
        return dtoObjectMapper.readValue(json, AttributeValue.serializableBuilderClass()).build();
    }

    private static boolean isNullValue(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value) {
        return nonNull(value.isNULL()) && value.isNULL();
    }

    private static String writeAsString(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue attributeValue)
        throws JsonProcessingException {
        return dtoObjectMapper.writeValueAsString(attributeValue);
    }
}
