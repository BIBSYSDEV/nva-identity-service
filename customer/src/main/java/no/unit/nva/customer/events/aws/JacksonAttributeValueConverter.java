package no.unit.nva.customer.events.aws;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class JacksonAttributeValueConverter implements AttributeValueConverter {

    private static final com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue EMPTY_ATTRIBUTE_VALUE = new com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue();

    public JacksonAttributeValueConverter() {
    }

    @Override
    public Map<String, AttributeValue> convert(
        Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> image
    ) {
        return image.entrySet().stream()
                   .collect(Collectors.toMap(
                       Entry::getKey,
                       entry -> convertValue(entry.getValue())
                   ));
    }

    private AttributeValue convertValue(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue awsValue
    ) {
        return attempt(() -> toSdkAttributeValue(awsValue)).orElseThrow();
    }

    private AttributeValue toSdkAttributeValue(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value
    ) throws JsonProcessingException {
        if (isNullValue(value)) {
            return AttributeValue.builder().nul(true).build();
        }

        if (containsAttributeValueMap(value)) {
            return convertNestedMap(value);
        }

        if (nonNull(value.getL()) && value.getL().isEmpty()) {
            return AttributeValue.builder().l(emptyList()).build();
        }

        return convertViaJson(value);
    }

    private AttributeValue convertNestedMap(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value
    ) {
        var convertedMap = value.getM().entrySet().stream()
                               .collect(Collectors.toMap(
                                   Entry::getKey,
                                   entry -> convertValue(entry.getValue())
                               ));
        return AttributeValue.builder().m(convertedMap).build();
    }

    private AttributeValue convertViaJson(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value
    ) throws JsonProcessingException {
        var json = dtoObjectMapper.writeValueAsString(value);
        return dtoObjectMapper.readValue(json, AttributeValue.serializableBuilderClass()).build();
    }

    private boolean isNullValue(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value
    ) {
        return Boolean.TRUE.equals(value.isNULL())
               || isAttributeWhereAllValuesAreNull(value)
               || nonNull(value.getS()) && Optional.of(value)
                      .map(com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue::getS)
                      .filter(not(String::isBlank))
                      .isEmpty();
    }

    private boolean isAttributeWhereAllValuesAreNull(com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value) {
        return EMPTY_ATTRIBUTE_VALUE.equals(value);
    }

    private boolean containsAttributeValueMap(
        com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue value
    ) {
        return value.getM() != null;
    }
}
