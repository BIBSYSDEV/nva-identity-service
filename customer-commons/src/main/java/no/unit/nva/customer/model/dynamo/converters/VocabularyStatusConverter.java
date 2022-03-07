package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.VocabularyStatus;
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

    @Override
    public EnhancedType<VocabularyStatus> type() {
        return null;
    }

    @Override
    public AttributeValueType attributeValueType() {
        return null;
    }
}
