package no.unit.nva.customer.model.dynamo.converters;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;

public class ServiceCenterStringProvider implements AttributeConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (String.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) new StringAttributeConverter();
        } else {
            return null;
        }
    }
}
