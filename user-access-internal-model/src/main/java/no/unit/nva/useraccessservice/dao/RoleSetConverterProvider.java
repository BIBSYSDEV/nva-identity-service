package no.unit.nva.useraccessservice.dao;

import java.util.Set;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class RoleSetConverterProvider implements AttributeConverterProvider {

    public static final RoleSetConverter ROLE_SET_CONVERTER = new RoleSetConverter();

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (isSet(enhancedType) && parametricTypeIsRoleDb(enhancedType)) {
            return (AttributeConverter<T>) ROLE_SET_CONVERTER;
        }

        return null;
    }

    private <T> boolean parametricTypeIsRoleDb(EnhancedType<T> enhancedType) {
        return enhancedType.rawClassParameters().size() == 1
                && EnhancedType.of(RoleDb.class).equals(enhancedType.rawClassParameters().get(0));
    }

    private <T> boolean isSet(EnhancedType<T> enhancedType) {
        return Set.class.equals(enhancedType.rawClass());
    }
}
