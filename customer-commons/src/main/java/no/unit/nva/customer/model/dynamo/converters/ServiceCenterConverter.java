package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.CustomerDao.ServiceCenterDao;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ServiceCenterConverter implements AttributeConverter<ServiceCenterDao> {

    public static final TableSchema<ServiceCenterDao> TABLE_SCHEMA =
        TableSchema.fromBean(ServiceCenterDao.class);

    public ServiceCenterConverter() {
    }

    @Override
    public AttributeValue transformFrom(ServiceCenterDao input) {
        var map = TABLE_SCHEMA.itemToMap(input, true);
        return AttributeValue.builder().m(map).build();
    }

    @Override
    public ServiceCenterDao transformTo(AttributeValue input) {
        return TABLE_SCHEMA.mapToItem(input.m());
    }

    @JacocoGenerated
    @Override
    public EnhancedType<ServiceCenterDao> type() {
        return EnhancedType.of(ServiceCenterDao.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}