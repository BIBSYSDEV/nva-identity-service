package no.unit.nva.customer.events.aws;

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;

@FunctionalInterface
public interface AttributeValueConverter {

    Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> convert(
        Map<String, AttributeValue> input);
}
