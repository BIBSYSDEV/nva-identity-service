package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.CustomerDao.ServiceCenterDao;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class ServiceCenterConverterProvider implements AttributeConverterProvider {


    private final ServiceCenterConverter converter = new ServiceCenterConverter();

    @SuppressWarnings("unchecked")
    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        if (ServiceCenterDao.class.isAssignableFrom(enhancedType.rawClass())) {
            return (AttributeConverter<T>) converter;
        } else {
            return null;
        }
    }
}