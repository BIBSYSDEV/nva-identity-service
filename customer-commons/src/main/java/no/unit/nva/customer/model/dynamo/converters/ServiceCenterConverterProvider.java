package no.unit.nva.customer.model.dynamo.converters;

import java.net.URI;
import no.unit.nva.customer.model.CustomerDao.ServiceCenterDao;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UriAttributeConverter;

public class ServiceCenterConverterProvider implements AttributeConverterProvider {


    private final ServiceCenterConverter converter = new ServiceCenterConverter();

    @SuppressWarnings("unchecked")
    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (ServiceCenterDao.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) converter;
        }
        if (String.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) new StringAttributeConverter();
        }
        if (URI.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) new UriAttributeConverter();
        } else {
            return null;
        }
    }
}