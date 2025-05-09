package no.unit.nva.customer.events.producer;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.customer.events.model.ChannelClaimUpdateEvents.addedChannelClaim;
import static no.unit.nva.customer.events.model.ChannelClaimUpdateEvents.removedChannelClaim;
import static no.unit.nva.customer.events.model.ChannelClaimUpdateEvents.updatedChannelClaim;
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
import java.util.stream.Stream;
import no.unit.nva.customer.events.aws.AttributeValueConverter;
import no.unit.nva.customer.events.model.ChannelClaim;
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

    private boolean isModified(ChannelClaimDto oldClaim, ChannelClaimDto newClaim) {
        return !Objects.equals(oldClaim, newClaim);
    }

    private List<ResourceUpdateEvent<ChannelClaim>> computeChannelClaimEvents(URI customerId,
                                                                              URI organizationId,
                                                                              Collection<ChannelClaimDto> oldClaims,
                                                                              Collection<ChannelClaimDto> newClaims) {
        var oldMap = oldClaims.stream()
                         .collect(Collectors.toMap(ChannelClaimDto::channel, Function.identity()));

        var newMap = newClaims.stream()
                         .collect(Collectors.toMap(ChannelClaimDto::channel, Function.identity()));

        return Stream.of(
                computeAddedEvents(customerId, organizationId, newMap, oldMap),
                computeRemovedEvents(customerId, organizationId, oldMap, newMap),
                computeModifiedEvents(customerId, organizationId, oldMap, newMap)
            )
                   .flatMap(Collection::stream)
                   .toList();
    }

    private List<ResourceUpdateEvent<ChannelClaim>> computeModifiedEvents(URI customerId,
                                                                          URI organizationId,
                                                                          Map<URI, ChannelClaimDto> oldMap,
                                                                          Map<URI, ChannelClaimDto> newMap) {
        var events = new ArrayList<ResourceUpdateEvent<ChannelClaim>>();
        for (var key : oldMap.keySet()) {
            var oldClaim = oldMap.get(key);
            var newClaim = newMap.get(key);
            if (nonNull(newClaim) && isModified(oldClaim, newClaim)) {
                events.add(updatedChannelClaim(customerId, organizationId, newClaim));
            }
        }

        return events;
    }

    private static List<ResourceUpdateEvent<ChannelClaim>> computeRemovedEvents(URI customerId, URI organizationId,
                                                                                Map<URI, ChannelClaimDto> oldMap,
                                                                                Map<URI, ChannelClaimDto> newMap) {
        return oldMap.keySet().stream()
                   .filter(not(newMap::containsKey))
                   .map(key -> removedChannelClaim(customerId, organizationId,
                                                   oldMap.get(key)))
                   .toList();
    }

    private static List<ResourceUpdateEvent<ChannelClaim>> computeAddedEvents(URI customerId,
                                                                              URI organizationId,
                                                                              Map<URI, ChannelClaimDto> newMap,
                                                                              Map<URI, ChannelClaimDto> oldMap) {
        return newMap.keySet().stream()
                   .filter(not(oldMap::containsKey))
                   .map(key -> addedChannelClaim(customerId, organizationId, newMap.get(key)))
                   .toList();
    }

    private Optional<CustomerDto> convertToCustomerDto(Map<String, AttributeValue> image) {
        return Optional.ofNullable(image)
                   .map(attributeValueConverter::convert)
                   .map(CUSTOMER_DAO_TABLE_SCHEMA::mapToItem)
                   .map(CustomerDao::toCustomerDto);
    }
}
