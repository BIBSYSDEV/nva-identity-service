package no.unit.nva.customer.model.dynamo.converters;

import java.net.URI;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UriAttributeConverter;

public class ServiceCenterUriProvider implements AttributeConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (URI.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) new UriAttributeConverter();
        } else {
            return null;
        }
    }
}
