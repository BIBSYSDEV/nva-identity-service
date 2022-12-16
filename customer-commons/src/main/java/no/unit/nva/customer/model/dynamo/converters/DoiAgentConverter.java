package no.unit.nva.customer.model.dynamo.converters;

import no.unit.nva.customer.model.CustomerDao.DoiAgentDao;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@JacocoGenerated
public class DoiAgentConverter implements AttributeConverter<DoiAgentDao> {

    public static final TableSchema<DoiAgentDao> TABLE_SCHEMA = TableSchema.fromBean(DoiAgentDao.class);

    public DoiAgentConverter() {
    }

    @Override
    public EnhancedType<DoiAgentDao> type() {
        return EnhancedType.of(DoiAgentDao.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }

    @Override
    public AttributeValue transformFrom(DoiAgentDao input) {
        var map = TABLE_SCHEMA.itemToMap(input, true);
        return AttributeValue.builder().m(map).build();
    }

    @Override
    public DoiAgentDao transformTo(AttributeValue input) {
        return TABLE_SCHEMA.mapToItem(input.m());
    }
}