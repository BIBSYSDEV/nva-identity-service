package no.unit.nva.customer.events.producer;

import static nva.commons.core.attempt.Try.attempt;
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
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.events.aws.AttributeValueConverter;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ChannelClaimUpdateEvents;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.model.CustomerDao;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.channelclaim.ChannelClaimDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class DefaultCustomerResourceUpdateEventsProducer implements CustomerResourceUpdateEventsProducer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCustomerResourceUpdateEventsProducer.class);
    private static final TableSchema<CustomerDao> CUSTOMER_DAO_TABLE_SCHEMA = TableSchema.fromBean(CustomerDao.class);

    private final AttributeValueConverter attributeValueConverter;

    public DefaultCustomerResourceUpdateEventsProducer(AttributeValueConverter attributeValueConverter) {
        this.attributeValueConverter = attributeValueConverter;
    }

    @Override
    public List<ResourceUpdateEvent<ChannelClaim>> produceEvents(DynamodbStreamRecord record) {
        logger.info("Processing record: {}", record);

        var oldCustomerOpt = convertToCustomerDto(record.getDynamodb().getOldImage());
        var newCustomerOpt = convertToCustomerDto(record.getDynamodb().getNewImage());

        if (oldCustomerOpt.isEmpty() && newCustomerOpt.isEmpty()) {
            return List.of();
        }

        var customer = newCustomerOpt.or(() -> oldCustomerOpt).orElseThrow();
        var customerId = customer.getId();
        var organizationId = customer.getCristinId();

        var oldClaims = oldCustomerOpt.map(CustomerDto::getChannelClaims).orElse(List.of());
        var newClaims = newCustomerOpt.map(CustomerDto::getChannelClaims).orElse(List.of());

        return computeChannelClaimEvents(customerId, organizationId, oldClaims, newClaims);
    }

    private String claimKey(ChannelClaimDto claim) {
        return claim.channel().toString(); // Adjust fields as needed
    }

    private boolean isModified(ChannelClaimDto oldClaim, ChannelClaimDto newClaim) {
        return !Objects.equals(oldClaim, newClaim);
    }

    private List<ResourceUpdateEvent<ChannelClaim>> computeChannelClaimEvents(URI customerId,
                                                                                 URI organizationId,
                                                                                 Collection<ChannelClaimDto> oldClaims,
                                                                                 Collection<ChannelClaimDto> newClaims) {
        var oldMap = oldClaims.stream()
                         .collect(Collectors.toMap(this::claimKey, Function.identity()));

        var newMap = newClaims.stream()
                         .collect(Collectors.toMap(this::claimKey, Function.identity()));

        var events = new ArrayList<ResourceUpdateEvent<ChannelClaim>>();

        // additions
        for (var entry : newMap.entrySet()) {
            if (!oldMap.containsKey(entry.getKey())) {
                events.add(ChannelClaimUpdateEvents.addedChannelClaim(customerId, organizationId, entry.getValue()));
            }
        }

        // removals
        for (var entry : oldMap.entrySet()) {
            if (!newMap.containsKey(entry.getKey())) {
                events.add(ChannelClaimUpdateEvents.removedChannelClaim(customerId, organizationId, entry.getValue()));
            }
        }

        // modifications
        for (var key : oldMap.keySet()) {
            var oldClaim = oldMap.get(key);
            var newClaim = newMap.get(key);
            if (newClaim != null && isModified(oldClaim, newClaim)) {
                events.add(ChannelClaimUpdateEvents.updatedChannelClaim(customerId, organizationId, newClaim));
            }
        }

        var eventsAsJson = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(events)).orElseThrow();
        logger.info("Derived the following events from dynamodb stream: {}", eventsAsJson);
        return events;
    }

    private Optional<CustomerDto> convertToCustomerDto(Map<String, AttributeValue> image) {
        return Optional.ofNullable(image)
                   .map(attributeValueConverter::convert)
                   .map(CUSTOMER_DAO_TABLE_SCHEMA::mapToItem)
                   .map(CustomerDao::toCustomerDto);
    }

}
