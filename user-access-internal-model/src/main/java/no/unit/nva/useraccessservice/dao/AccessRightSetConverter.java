package no.unit.nva.useraccessservice.dao;

import static nva.commons.core.exceptions.ExceptionUtils.stackTraceInSingleLine;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AccessRightSetConverter implements AttributeConverter<Set<AccessRight>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightSetConverter.class);
    
    @Override
    public AttributeValue transformFrom(Set<AccessRight> input) {
        var stringCollection = collectToSetOfStrings(input);
        return !stringCollection.isEmpty()
                   ? AttributeValue.builder().ss(stringCollection).build()
                   : AttributeValue.builder().nul(true).build();
    }

    @Override
    public Set<AccessRight> transformTo(AttributeValue input) {
        return input.ss().stream()
                   .map(this::optionalAccessRightFromString)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toSet());
    }

    private Optional<AccessRight> optionalAccessRightFromString(String s) {
        var accessRight = AccessRight.fromPersistedStringOptional(s);
        if (accessRight.isEmpty()) {
            LOGGER.warn(stackTraceInSingleLine(new Exception("Could not find AccessRight for string: " + s)));
        }
        return accessRight;
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
            .map(AccessRight::toPersistedString)
            .collect(Collectors.toSet());
    }
}
