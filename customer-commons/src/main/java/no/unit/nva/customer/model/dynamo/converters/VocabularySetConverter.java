package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.VocabularyDao;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.unit.nva.customer.model.dynamo.converters.DynamoUtils.nonEmpty;

public class VocabularySetConverter implements AttributeConverter<Set<VocabularyDao>> {

    public static final boolean IGNORE_NULLS = true;

    @Override
    public AttributeValue transformFrom(Set<VocabularyDao> input) {
        var attributeValues = Optional.ofNullable(input)
                .stream()
                .flatMap(Collection::stream)
                .map(collectionElement -> VocabularyDao.SCHEMA.itemToMap(collectionElement, IGNORE_NULLS))
                .map(attributeMap -> AttributeValue.builder().m(attributeMap).build())
                .collect(Collectors.toList());
        return nonEmpty(attributeValues) ? nonEmptyList(attributeValues) : null;
    }

    @Override
    public Set<VocabularyDao> transformTo(AttributeValue input) {
        return Optional.ofNullable(input)
                .stream()
                .map(AttributeValue::l)
                .flatMap(Collection::stream)
                .map(AttributeValue::m)
                .map(VocabularyDao.SCHEMA::mapToItem)
                .collect(Collectors.toSet());
    }

    @JacocoGenerated
    @Override
    public EnhancedType<Set<VocabularyDao>> type() {
        return EnhancedType.setOf(VocabularyDao.class);
    }

    @JacocoGenerated
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.BS;
    }

    private AttributeValue nonEmptyList(List<AttributeValue> attributeValues) {
        return AttributeValue.builder().l(attributeValues).build();
    }
}
