package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.VocabularyStatus;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class VocabularyStatusConverter implements AttributeConverter<VocabularyStatus> {

    @Override
    public AttributeValue transformFrom(VocabularyStatus input) {
        return AttributeValue.builder().s(input.getValue()).build();
    }

    @Override
    public VocabularyStatus transformTo(AttributeValue input) {
        return VocabularyStatus.lookUp(input.s());
    }

    @JacocoGenerated
    @Override
    public EnhancedType<VocabularyStatus> type() {
        return EnhancedType.of(VocabularyStatus.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
