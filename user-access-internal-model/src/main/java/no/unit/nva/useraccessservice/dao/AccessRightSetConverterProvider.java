package no.unit.nva.useraccessservice.dao;

import nva.commons.apigateway.AccessRight;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

import java.util.Set;

public class AccessRightSetConverterProvider implements AttributeConverterProvider {

    public static final AccessRightSetConverter ACCESS_RIGHT_SET_CONVERTER = new AccessRightSetConverter();

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (isSet(enhancedType) && parametricTypeIsAccessRight(enhancedType)) {
            return (AttributeConverter<T>) ACCESS_RIGHT_SET_CONVERTER;
        }

        return null;
    }

    private <T> boolean parametricTypeIsAccessRight(EnhancedType<T> enhancedType) {
        return enhancedType.rawClassParameters().size() == 1
            && EnhancedType.of(AccessRight.class).equals(enhancedType.rawClassParameters().get(0));
    }

    private <T> boolean isSet(EnhancedType<T> enhancedType) {
        return Set.class.equals(enhancedType.rawClass());
    }
}
