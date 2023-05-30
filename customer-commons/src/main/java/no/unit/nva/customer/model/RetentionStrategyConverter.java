package no.unit.nva.customer.model;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class RetentionStrategyConverter implements AttributeConverter<RetentionStrategyDao> {

    public static final TableSchema<RetentionStrategyDao> TABLE_SCHEMA = TableSchema.fromBean(RetentionStrategyDao.class);

    public RetentionStrategyConverter() {
    }

    @Override
    public AttributeValue transformFrom(RetentionStrategyDao input) {
        var map = TABLE_SCHEMA.itemToMap(input, true);
        return AttributeValue.builder().m(map).build();
    }

    @Override
    public RetentionStrategyDao transformTo(AttributeValue input) {
        return TABLE_SCHEMA.mapToItem(input.m());
    }

    @Override
    public EnhancedType<RetentionStrategyDao> type() {
        return  EnhancedType.of(RetentionStrategyDao.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}
