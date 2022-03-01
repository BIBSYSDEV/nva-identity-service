package no.unit.nva.useraccessmanagement.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class RoleSetConverter implements AttributeConverter<Set<RoleDb>> {

    @Override
    public AttributeValue transformFrom(Set<RoleDb> input) {
        var items = Optional.ofNullable(input).stream()
            .flatMap(Collection::stream)
            .map(role -> RoleDb.TABLE_SCHEMA.itemToMap(role, true))
            .map(role -> AttributeValue.builder().m(role).build())
            .collect(Collectors.toList());
        return AttributeValue.builder().l(items).build();
    }

    @Override
    public Set<RoleDb> transformTo(AttributeValue input) {
        if (input.hasL()) {
            return input.l().stream()
                .map(AttributeValue::m)
                .map(map -> RoleDb.TABLE_SCHEMA.mapToItem(map))
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    @JacocoGenerated
    @Override
    public EnhancedType<Set<RoleDb>> type() {
        return EnhancedType.setOf(RoleDb.class);
    }

    @JacocoGenerated
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.BS;
    }
}


