package no.unit.nva.customer.events.handler;

import static no.unit.nva.customer.model.PublicationInstanceTypes.ARTISTIC_DEGREE_PHD;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_BACHELOR;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_LICENTIATE;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_MASTER;
import static no.unit.nva.customer.model.PublicationInstanceTypes.DEGREE_PHD;
import static no.unit.nva.customer.model.PublicationInstanceTypes.OTHER_STUDENT_WORK;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.EVERYONE;
import static no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy.OWNER_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ChannelClaim.Constraints;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomerTableDynamodbStreamToEventBridgeHandlerTest {

    private static final Set<PublicationInstanceTypes> DEFAULT_SCOPE = Set.of(
        DEGREE_PHD,
        DEGREE_MASTER,
        DEGREE_BACHELOR,
        DEGREE_LICENTIATE,
        ARTISTIC_DEGREE_PHD,
        OTHER_STUDENT_WORK
    );

    private CustomerTableDynamodbStreamToEventBridgeHandler handler;

    @BeforeEach
    void beforeEach() {
        handler = new CustomerTableDynamodbStreamToEventBridgeHandler();
    }

    @Test
    void shouldHandleEmptyBatch() {
        var event = new DynamodbEvent();
        event.setRecords(Collections.emptyList());

        assertDoesNotThrow(() -> handler.handleRequest(event, new FakeContext()));
    }

    @Test
    void shouldEmitEventForNewChannelClaimAddedOnCreationOfNewCustomer() {
        var channelIdentifier = UUID.randomUUID();
        var channelId = channelId(channelIdentifier);
        var customerIdentifier = UUID.randomUUID();
        var organizationId = randomOrganizationId();

        var event = customerCreationEventWithChannelClaim(customerIdentifier, organizationId, channelId);

        var result = handler.handleRequest(event, new FakeContext());

        var customerId = customerId(customerIdentifier);
        var channelClaimId = channelClaimId(channelIdentifier);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.ADDED,
                                                            channelClaimId,
                                                            customerId,
                                                            organizationId,
                                                            channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForNewChannelClaimAddedOnCustomerUpdate() {
        var channelIdentifier = UUID.randomUUID();
        var channelId = channelId(channelIdentifier);
        var customerIdentifier = UUID.randomUUID();
        var organizationId = randomOrganizationId();

        var event = customerUpdateEvent(customerIdentifier,
                                        organizationId,
                                        channelId,
                                        DEFAULT_SCOPE,
                                        OWNER_ONLY,
                                        OWNER_ONLY);

        var result = handler.handleRequest(event, new FakeContext());

        var customerId = customerId(customerIdentifier);
        var channelClaimId = channelClaimId(channelIdentifier);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.UPDATED,
                                                            channelClaimId,
                                                            customerId,
                                                            organizationId,
                                                            channelId,
                                                            DEFAULT_SCOPE,
                                                            OWNER_ONLY,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForChannelClaimDeletedOnCustomerUpdate() {
        var channelIdentifier = UUID.randomUUID();
        var channelId = channelId(channelIdentifier);
        var customerIdentifier = UUID.randomUUID();
        var organizationId = randomOrganizationId();

        var event = customerUpdateEvent(customerIdentifier, organizationId, channelId);

        var result = handler.handleRequest(event, new FakeContext());

        var customerId = customerId(customerIdentifier);
        var channelClaimId = channelClaimId(channelIdentifier);
        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.REMOVED,
                                                            channelClaimId,
                                                            customerId,
                                                            organizationId,
                                                            channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForChannelClaimDeletedOnCustomerDeletion() {
        var channelIdentifier = UUID.randomUUID();
        var channelId = channelId(channelIdentifier);
        var customerIdentifier = UUID.randomUUID();
        var organizationId = randomOrganizationId();
        var event = customerDeletedEvent(customerIdentifier,
                                         organizationId,
                                         channelId);

        var result = handler.handleRequest(event, new FakeContext());

        var customerId = customerId(customerIdentifier);
        var channelClaimId = channelClaimId(channelIdentifier);
        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.REMOVED,
                                                            channelClaimId,
                                                            customerId,
                                                            organizationId,
                                                            channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    private URI randomOrganizationId() {
        var identifier = new Random().ints(4, 1, 9)
                             .mapToObj(String::valueOf)
                             .collect(Collectors.joining("."));
        var organizationId =
            "https://localhost/cristin/organization/" + identifier;
        return URI.create(organizationId);
    }

    private DynamodbEvent customerCreationEventWithChannelClaim(UUID customerIdentifier,
                                                                URI organizationId,
                                                                URI channelId) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(null);
        streamRecord.setNewImage(customerAttributeMap(customerIdentifier,
                                                      organizationId,
                                                      channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerUpdateEvent(UUID customerIdentifier,
                                              URI organizationId,
                                              URI channelId,
                                              Set<PublicationInstanceTypes> newScope,
                                              ChannelConstraintPolicy newPublishingPolicy,
                                              ChannelConstraintPolicy newEditingPolicy) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(customerIdentifier,
                                                      organizationId,
                                                      channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(customerAttributeMap(customerIdentifier,
                                                      organizationId,
                                                      channelId,
                                                      newScope,
                                                      newPublishingPolicy,
                                                      newEditingPolicy));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerDeletedEvent(UUID customerIdentifier,
                                               URI organizationId,
                                               URI channelId) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(customerIdentifier,
                                                      organizationId,
                                                      channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(null);

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerUpdateEvent(UUID customerIdentifier,
                                              URI organizationId,
                                              URI channelId) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(customerIdentifier,
                                                      organizationId,
                                                      channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(customerAttributeMap(customerIdentifier, organizationId));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private ResourceUpdateEvent<ChannelClaim> expectedEvent(ResourceUpdateEvent.Action action,
                                                            URI channelClaimId,
                                                            URI customerId,
                                                            URI organizationId,
                                                            URI channelId,
                                                            Set<PublicationInstanceTypes> scope,
                                                            ChannelConstraintPolicy publishingPolicy,
                                                            ChannelConstraintPolicy editingPolicy) {
        var constraint = new Constraints(scope,
                                         publishingPolicy,
                                         editingPolicy);
        var channelClaim = new ChannelClaim(channelClaimId, channelId, customerId, organizationId, constraint);
        return new ResourceUpdateEvent<>(action, "ChannelClaim", channelClaim);
    }

    private URI channelClaimId(UUID identifier) {
        var channelClaimId =
            "https://localhost/customer/channel-claim/" + identifier;
        return URI.create(channelClaimId);
    }

    private URI channelId(UUID identifier) {
        var channelId =
            "https://localhost/publication-channels-v2/serial-publication/" + identifier;
        return URI.create(channelId);
    }

    private URI customerId(UUID identifier) {
        var customerId =
            "https://localhost/customer/" + identifier;
        return URI.create(customerId);
    }

    private Map<String, AttributeValue> customerAttributeMap(UUID identifier,
                                                             URI organizationId,
                                                             URI channelId,
                                                             Set<PublicationInstanceTypes> scope,
                                                             ChannelConstraintPolicy publishingPolicy,
                                                             ChannelConstraintPolicy editingPolicy) {
        return Map.of(
            "identifier", new AttributeValue().withS(identifier.toString()),
            "cristinId", new AttributeValue().withS(organizationId.toString()),
            "channelClaims",
            new AttributeValue().withL(
                new AttributeValue().withM(channelClaimAttributeMap(channelId,
                                                                    scope,
                                                                    publishingPolicy,
                                                                    editingPolicy)))
        );
    }

    private Map<String, AttributeValue> customerAttributeMap(UUID identifier, URI organizationId) {
        return Map.of(
            "identifier", new AttributeValue().withS(identifier.toString()),
            "cristinId", new AttributeValue().withS(organizationId.toString())
        );
    }

    private Map<String, AttributeValue> channelClaimAttributeMap(URI channelId,
                                                                 Set<PublicationInstanceTypes> scope,
                                                                 ChannelConstraintPolicy publishingPolicy,
                                                                 ChannelConstraintPolicy editingPolicy) {
        return Map.of(
            "channel", new AttributeValue().withS(channelId.toString()),
            "constraint", new AttributeValue().withM(constraintAttributeMap(scope, publishingPolicy, editingPolicy))
        );
    }

    private Map<String, AttributeValue> constraintAttributeMap(Set<PublicationInstanceTypes> scope,
                                                               ChannelConstraintPolicy publishingPolicy,
                                                               ChannelConstraintPolicy editingPolicy) {
        var serializedScope = scope.stream()
                                  .map(PublicationInstanceTypes::name)
                                  .map(name -> new AttributeValue().withS(name))
                                  .toList();
        return Map.of("scope", new AttributeValue().withL(serializedScope),
                      "publishingPolicy", new AttributeValue().withS(publishingPolicy.name()),
                      "editingPolicy", new AttributeValue().withS(editingPolicy.name()));
    }
}
