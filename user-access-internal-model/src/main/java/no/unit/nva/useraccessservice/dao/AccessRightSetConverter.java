package no.unit.nva.useraccessservice.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AccessRightSetConverter implements AttributeConverter<Set<AccessRight>> {

    @Override
    public AttributeValue transformFrom(Set<AccessRight> input) {
        var stringCollection = collectToSetOfStrings(input);
        return !stringCollection.isEmpty() ?
                   AttributeValue.builder().ss(stringCollection).build()
                   : AttributeValue.builder().nul(true).build();
    }

    @Override
    public Set<AccessRight> transformTo(AttributeValue input) {
        return input.ss().stream()
            .map(AccessRight::fromString)
            .collect(Collectors.toSet());
    }

    @Override
    @JacocoGenerated
    public EnhancedType<Set<AccessRight>> type() {
        return EnhancedType.setOf(AccessRight.class);
    }

    @Override
    @JacocoGenerated
    public AttributeValueType attributeValueType() {
        return AttributeValueType.SS;
    }

    private Set<String> collectToSetOfStrings(Set<AccessRight> input) {
        return Optional.ofNullable(input)
            .stream()
            .flatMap(Collection::stream)
            .map(AccessRight::toString)
            .collect(Collectors.toSet());
    }
}
