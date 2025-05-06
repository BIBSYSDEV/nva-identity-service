package no.unit.nva.customer.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.customer.model.CustomerDao;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;

public class CustomerTableDynamodbStreamToEventBridgeHandler implements RequestHandler<DynamodbEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTableDynamodbStreamToEventBridgeHandler.class);

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
    }

    @Override
    public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        dynamodbEvent.getRecords().forEach(this::processRecord);
        return null;
    }

    private void processRecord(DynamodbStreamRecord record) {
        logger.info("Processing record: {}", record);

        var oldImage = record.getDynamodb().getOldImage();
        var newImage = record.getDynamodb().getNewImage();

        var tableSchema = TableSchema.fromBean(CustomerDao.class);
        var oldDao = convertToCustomerDao(oldImage, tableSchema);
        var newDao = convertToCustomerDao(newImage, tableSchema);

        logger.info("Old image: {}, new image: {}",
                    oldDao.orElse(null),
                    newDao.orElse(null));
    }

    private static Optional<CustomerDao> convertToCustomerDao(Map<String, AttributeValue> oldImage,
                                                              BeanTableSchema<CustomerDao> tableSchema) {
        return Optional.ofNullable(oldImage)
                   .map(AttributeValueConverter::mapToDynamoDbAttributeValue)
                   .map(tableSchema::mapToItem);
    }
}
