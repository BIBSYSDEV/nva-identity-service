package no.unit.nva.customer.model.dynamo.converters;

import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import no.unit.nva.customer.model.CustomerDao.RightsRetentionStrategyDao;

@JacocoGenerated
public class RetentionStrategyConverter implements AttributeConverter<RightsRetentionStrategyDao> {

    public static final TableSchema<RightsRetentionStrategyDao> TABLE_SCHEMA =
        TableSchema.fromBean(RightsRetentionStrategyDao.class);

    public RetentionStrategyConverter() {
    }

    @Override
    public AttributeValue transformFrom(RightsRetentionStrategyDao input) {
        var map = TABLE_SCHEMA.itemToMap(input, true);
        return AttributeValue.builder().m(map).build();
    }

    @Override
    public RightsRetentionStrategyDao transformTo(AttributeValue input) {
        return TABLE_SCHEMA.mapToItem(input.m());
    }

    @Override
    public EnhancedType<RightsRetentionStrategyDao> type() {
        return EnhancedType.of(RightsRetentionStrategyDao.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }
}
