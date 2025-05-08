package no.unit.nva.customer.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class CustomerTableDynamodbStreamToEventBridgeHandler
    implements RequestHandler<DynamodbEvent, List<EventData>> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTableDynamodbStreamToEventBridgeHandler.class);
    private static final TableSchema<CustomerDao> CUSTOMER_DAO_TABLE_SCHEMA = TableSchema.fromBean(CustomerDao.class);

    @JacocoGenerated
    public CustomerTableDynamodbStreamToEventBridgeHandler() {
    }

    @Override
    public List<EventData> handleRequest(DynamodbEvent dynamodbEvent, Context context) {
        return dynamodbEvent.getRecords().stream()
                   .map(this::processRecord)
                   .flatMap(Collection::stream)
                   .toList();
    }

    private List<EventData> processRecord(DynamodbStreamRecord record) {
        logger.info("Processing record: {}", record);

        var oldCustomerOpt = convertToCustomerDto(record.getDynamodb().getOldImage());
        var newCustomerOpt = convertToCustomerDto(record.getDynamodb().getNewImage());

        if (oldCustomerOpt.isEmpty() && newCustomerOpt.isEmpty()) {
            return List.of();
        }

        var customerId = newCustomerOpt.or(() -> oldCustomerOpt).get().getId();

        var oldClaims = oldCustomerOpt.map(CustomerDto::getChannelClaims).orElse(List.of());
        var newClaims = newCustomerOpt.map(CustomerDto::getChannelClaims).orElse(List.of());

        return computeChannelClaimEvents(customerId, oldClaims, newClaims);
    }

    private String claimKey(ChannelClaimDto claim) {
        return claim.channel().toString(); // Adjust fields as needed
    }

    private List<EventData> computeChannelClaimEvents(URI customerId, Collection<ChannelClaimDto> oldClaims,
                                                      Collection<ChannelClaimDto> newClaims) {
        var oldMap = oldClaims.stream()
                         .collect(Collectors.toMap(this::claimKey, Function.identity()));

        var newMap = newClaims.stream()
                         .collect(Collectors.toMap(this::claimKey, Function.identity()));

        var events = new ArrayList<EventData>();

        // additions
        for (var entry : newMap.entrySet()) {
            if (!oldMap.containsKey(entry.getKey())) {
                events.add(EventData.addedChannelClaim(customerId, entry.getValue()));
            }
        }

        // removals
        for (var entry : oldMap.entrySet()) {
            if (!newMap.containsKey(entry.getKey())) {
                events.add(EventData.removedChannelClaim(customerId, entry.getValue()));
            }
        }

        // modifications
        for (var key : oldMap.keySet()) {
            var oldClaim = oldMap.get(key);
            var newClaim = newMap.get(key);
            if (newClaim != null && isModified(oldClaim, newClaim)) {
                events.add(EventData.updatedChannelClaim(customerId, newClaim));
            }
        }

        logger.info("Derived the following events from dynamodb stream: {}", events);
        return events;
    }

    private boolean isModified(ChannelClaimDto oldClaim, ChannelClaimDto newClaim) {
        return !Objects.equals(oldClaim, newClaim);
    }
    private static Optional<CustomerDto> convertToCustomerDto(Map<String, AttributeValue> image) {
        return Optional.ofNullable(image)
                   .map(AttributeValueConverter::mapToDynamoDbAttributeValue)
                   .map(CUSTOMER_DAO_TABLE_SCHEMA::mapToItem)
                   .map(CustomerDao::toCustomerDto);
    }
}
