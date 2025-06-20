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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
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
import no.unit.nva.customer.events.emitter.EventEmitterException;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ChannelClaim.Constraints;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.Environment;
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

    private FakeEventBridgeClient eventBridgeClient;
    private CustomerTableDynamodbStreamToEventBridgeHandler handler;
    private ChannelClaimContext channelClaimContext;
    private Environment environment;
    private Context context;

    @BeforeEach
    void beforeEach() {
        this.environment = mock(Environment.class);
        when(environment.readEnv("EVENT_BUS_NAME")).thenReturn("MY_EVENT_BUS_NAME");

        this.context = mock(Context.class);
        when(context.getInvokedFunctionArn()).thenReturn("FAKE_INVOKED_FUNCTION_ARN");

        this.eventBridgeClient = new FakeEventBridgeClient();
        this.handler = new CustomerTableDynamodbStreamToEventBridgeHandler(environment, eventBridgeClient);
        this.channelClaimContext = generateContext();
    }

    private ChannelClaimContext generateContext() {
        var channelIdentifier = UUID.randomUUID();
        var channelId = channelId(channelIdentifier);
        var customerIdentifier = UUID.randomUUID();
        var organizationId = randomOrganizationId();
        var customerId = customerId(customerIdentifier);
        var channelClaimId = channelClaimId(channelIdentifier);

        return new ChannelClaimContext(channelClaimId, channelIdentifier, channelId, customerIdentifier,
                                       customerId, organizationId);
    }

    @Test
    void shouldHandleEmptyBatch() {
        var event = new DynamodbEvent();
        event.setRecords(Collections.emptyList());

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
    }

    @Test
    void shouldThrowWhenEventBridgeClientFailsWithHttpStatus() {
        var event = customerCreationEventWithChannelClaim(channelClaimContext);

        handler = new CustomerTableDynamodbStreamToEventBridgeHandler(environment,
                                                                      new FailingEventBridgeClient(true));

        assertThrows(EventEmitterException.class, () -> handler.handleRequest(event, context));
    }

    @Test
    void shouldThrowWhenEventBridgeClientFailsOnIndividualEntries() {
        var event = customerCreationEventWithChannelClaim(channelClaimContext);

        handler = new CustomerTableDynamodbStreamToEventBridgeHandler(environment,
                                                                      new FailingEventBridgeClient(false));

        assertThrows(EventEmitterException.class, () -> handler.handleRequest(event, context));
    }

    @Test
    void shouldEmitEventForNewChannelClaimAddedOnCreationOfNewCustomer() {
        var event = customerCreationEventWithChannelClaim(channelClaimContext);

        var result = handler.handleRequest(event, context);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.ADDED,
                                                            channelClaimContext.channelClaimId,
                                                            channelClaimContext.customerId,
                                                            channelClaimContext.organizationId,
                                                            channelClaimContext.channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForNewChannelClaimAddedOnCustomerUpdate() {
        var event = customerUpdateEvent(channelClaimContext,
                                        DEFAULT_SCOPE,
                                        OWNER_ONLY,
                                        OWNER_ONLY);

        var result = handler.handleRequest(event, context);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.UPDATED,
                                                            channelClaimContext.channelClaimId,
                                                            channelClaimContext.customerId,
                                                            channelClaimContext.organizationId,
                                                            channelClaimContext.channelId,
                                                            DEFAULT_SCOPE,
                                                            OWNER_ONLY,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForChannelClaimDeletedOnCustomerUpdate() {
        var event = customerUpdateEvent(channelClaimContext);

        var result = handler.handleRequest(event, context);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.REMOVED,
                                                            channelClaimContext.channelClaimId,
                                                            channelClaimContext.customerId,
                                                            channelClaimContext.organizationId,
                                                            channelClaimContext.channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    @Test
    void shouldEmitEventForChannelClaimDeletedOnCustomerDeletion() {
        var event = customerDeletedEvent(channelClaimContext);

        var result = handler.handleRequest(event, context);

        assertThat(result, containsInAnyOrder(expectedEvent(ResourceUpdateEvent.Action.REMOVED,
                                                            channelClaimContext.channelClaimId,
                                                            channelClaimContext.customerId,
                                                            channelClaimContext.organizationId,
                                                            channelClaimContext.channelId,
                                                            DEFAULT_SCOPE,
                                                            EVERYONE,
                                                            OWNER_ONLY)));
    }

    @Test
    void some() {
        var string = """
            {
  eventID: 1d2b5cffe8e6ed78ef81a970f15629c6,
  eventName: MODIFY,
  eventVersion: 1.1,
  eventSource: aws
  :
  dynamodb,
  awsRegion: eu-west-1,
  dynamodb: {
    ApproximateCreationDateTime: Thu
    Jun
    19
    10: 38
    :
    52
    UTC
    2025,
    Keys: {
      identifier={
  S: a228aba6-932b-4f53-b2de-31ad8daf9f8d
}}, NewImage: {identifier={S: a228aba6-932b-4f53-b2de-31ad8daf9f8d,}, rightsRetentionStrategy={M: {type={S: NullRightsRetentionStrategy,}},}, displayName={S: Universitetet i Bergen,}, cristinId={S: https: //api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0,}, rorId={S: ,}, generalSupportEnabled={BOOL: true}, type={S: Customer,}, feideOrganizationDomain={S: uib.no,}, createdDate={S: 2023-07-07T07:35:33.765827Z,}, customerOf={S: nva.unit.no,}, nviInstitution={BOOL: true}, modifiedDate={S: 2025-06-19T10:38:52.785521737Z,}, name={S: Universitetet i Bergen,}, allowFileUploadForTypes={SS: [ACADEMIC_ARTICLE, ACADEMIC_CHAPTER, ACADEMIC_LITERATURE_REVIEW, ACADEMIC_MONOGRAPH, ARCHITECTURE, ARTISTIC_DESIGN, BOOK_ANTHOLOGY, CASE_REPORT, CHAPTER_CONFERENCE_ABSTRACT, CHAPTER_IN_REPORT, CONFERENCE_ABSTRACT, CONFERENCE_LECTURE, CONFERENCE_POSTER, CONFERENCE_REPORT, DATA_MANAGEMENT_PLAN, DATA_SET, DEGREE_BACHELOR, DEGREE_LICENTIATE, DEGREE_MASTER, DEGREE_PHD, ENCYCLOPEDIA, ENCYCLOPEDIA_CHAPTER, EXHIBITION_CATALOG, EXHIBITION_CATALOG_CHAPTER, EXHIBITION_PRODUCTION, INTRODUCTION, JOURNAL_CORRIGENDUM, JOURNAL_ISSUE, JOURNAL_LEADER, JOURNAL_LETTER, JOURNAL_REVIEW, LECTURE, LITERARY_ARTS, MAP, MEDIA_BLOG_POST, MEDIA_FEATURE_ARTICLE, MEDIA_INTERVIEW, MEDIA_PARTICIPATION_IN_RADIO_OR_TV, MEDIA_PODCAST, MEDIA_READER_OPINION, MOVING_PICTURE, MUSIC_PERFORMANCE, NON_FICTION_CHAPTER, NON_FICTION_MONOGRAPH, OTHER_PRESENTATION, OTHER_STUDENT_WORK, PERFORMING_ARTS, POPULAR_SCIENCE_ARTICLE, POPULAR_SCIENCE_CHAPTER, POPULAR_SCIENCE_MONOGRAPH, PROFESSIONAL_ARTICLE, REPORT_BASIC, REPORT_BOOK_OF_ABSTRACT, REPORT_RESEARCH, REPORT_WORKING_PAPER, REPOST_POLICY, STUDY_PROTOCOL, TEXTBOOK, TEXTBOOK_CHAPTER, VISUAL_ARTS],}, publicationWorkflow={S: REGISTRATOR_PUBLISHES_METADATA_ONLY,}, channelClaims={L: [{M: {channel={S: https://api.dev.nva.aws.unit.no/publication-channels-v2/publisher/CBCE38D7-C6C6-4CE9-BCED-D64610033E9B,}, constraint={M: {scope={L: [{S: DEGREE_BACHELOR,}, {S: DEGREE_MASTER,}, {S: DEGREE_PHD,}, {S: ARTISTIC_DEGREE_PHD,}, {S: DEGREE_LICENTIATE,}, {S: OTHER_STUDENT_WORK,}],}, publishingPolicy={S: EVERYONE,}, editingPolicy={S: OWNER_ONLY,}},}},}],}, serviceCenter={M: {name={NULL: true,}, uri={NULL: true,}},}, shortName={S: UIB,}, doiAgent={M: {},}, sector={S: UHI,}, rboInstitution={BOOL: false}},OldImage: {identifier={S: a228aba6-932b-4f53-b2de-31ad8daf9f8d,}, rightsRetentionStrategy={M: {type={S: NullRightsRetentionStrategy,}},}, displayName={S: Universitetet i Bergen,}, cristinId={S: https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0,}, rorId={S: ,}, generalSupportEnabled={BOOL: true}, type={S: Customer,}, feideOrganizationDomain={S: uib.no,}, createdDate={S: 2023-07-07T07:35:33.765827Z,}, customerOf={S: nva.unit.no,}, nviInstitution={BOOL: true}, modifiedDate={S: 2024-10-02T13:48:54.230396999Z,}, name={S: Universitetet i Bergen,}, allowFileUploadForTypes={SS: [ACADEMIC_ARTICLE, ACADEMIC_CHAPTER, ACADEMIC_LITERATURE_REVIEW, ACADEMIC_MONOGRAPH, ARCHITECTURE, ARTISTIC_DESIGN, BOOK_ANTHOLOGY, CASE_REPORT, CHAPTER_CONFERENCE_ABSTRACT, CHAPTER_IN_REPORT, CONFERENCE_ABSTRACT, CONFERENCE_LECTURE, CONFERENCE_POSTER, CONFERENCE_REPORT, DATA_MANAGEMENT_PLAN, DATA_SET, DEGREE_BACHELOR, DEGREE_LICENTIATE, DEGREE_MASTER, DEGREE_PHD, ENCYCLOPEDIA, ENCYCLOPEDIA_CHAPTER, EXHIBITION_CATALOG, EXHIBITION_CATALOG_CHAPTER, EXHIBITION_PRODUCTION, INTRODUCTION, JOURNAL_CORRIGENDUM, JOURNAL_ISSUE, JOURNAL_LEADER, JOURNAL_LETTER, JOURNAL_REVIEW, LECTURE, LITERARY_ARTS, MAP, MEDIA_BLOG_POST, MEDIA_FEATURE_ARTICLE, MEDIA_INTERVIEW, MEDIA_PARTICIPATION_IN_RADIO_OR_TV, MEDIA_PODCAST, MEDIA_READER_OPINION, MOVING_PICTURE, MUSIC_PERFORMANCE, NON_FICTION_CHAPTER, NON_FICTION_MONOGRAPH, OTHER_PRESENTATION, OTHER_STUDENT_WORK, PERFORMING_ARTS, POPULAR_SCIENCE_ARTICLE, POPULAR_SCIENCE_CHAPTER, POPULAR_SCIENCE_MONOGRAPH, PROFESSIONAL_ARTICLE, REPORT_BASIC, REPORT_BOOK_OF_ABSTRACT, REPORT_RESEARCH, REPORT_WORKING_PAPER, REPOST_POLICY, STUDY_PROTOCOL, TEXTBOOK, TEXTBOOK_CHAPTER, VISUAL_ARTS],}, publicationWorkflow={S: REGISTRATOR_PUBLISHES_METADATA_ONLY,}, serviceCenter={M: {name={NULL: true,}, uri={NULL: true,}},}, shortName={S: UIB,}, doiAgent={M: {},}, sector={S: UHI,}, rboInstitution={BOOL: false}},SequenceNumber: 4981317700000513153297190510,SizeBytes: 3474,StreamViewType: NEW_AND_OLD_IMAGES},eventSourceArn: arn:aws:dynamodb:eu-west-1:884807050265:table/nva-customers-master-pipelines-NvaIdentityService-WLJCBMUDMRYZ-nva-identity-service/stream/2025-05-09T11:18:51.458}
            """;
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();
        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));
    }

    private URI randomOrganizationId() {
        var identifier = new Random().ints(4, 1, 9)
                             .mapToObj(String::valueOf)
                             .collect(Collectors.joining("."));
        var organizationId =
            "https://localhost/cristin/organization/" + identifier;
        return URI.create(organizationId);
    }

    private DynamodbEvent customerCreationEventWithChannelClaim(ChannelClaimContext context) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(null);
        streamRecord.setNewImage(customerAttributeMap(context.customerIdentifier,
                                                      context.organizationId,
                                                      context.channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerUpdateEvent(ChannelClaimContext context,
                                              Set<PublicationInstanceTypes> newScope,
                                              ChannelConstraintPolicy newPublishingPolicy,
                                              ChannelConstraintPolicy newEditingPolicy) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(context.customerIdentifier,
                                                      context.organizationId,
                                                      context.channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(customerAttributeMap(context.customerIdentifier,
                                                      context.organizationId,
                                                      context.channelId,
                                                      newScope,
                                                      newPublishingPolicy,
                                                      newEditingPolicy));

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerDeletedEvent(ChannelClaimContext context) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(context.customerIdentifier,
                                                      context.organizationId,
                                                      context.channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(null);

        record.setDynamodb(streamRecord);

        event.setRecords(Collections.singletonList(record));

        return event;
    }

    private DynamodbEvent customerUpdateEvent(ChannelClaimContext context) {
        var event = new DynamodbEvent();

        var record = new DynamodbEvent.DynamodbStreamRecord();

        var streamRecord = new StreamRecord();

        streamRecord.setOldImage(customerAttributeMap(context.customerIdentifier,
                                                      context.organizationId,
                                                      context.channelId,
                                                      DEFAULT_SCOPE,
                                                      EVERYONE,
                                                      OWNER_ONLY));
        streamRecord.setNewImage(customerAttributeMap(context.customerIdentifier, context.organizationId));

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
            "rightsRetentionStrategy", new AttributeValue().withM(rightsRetentionStrategyArraibuteMap()),
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
            "cristinId", new AttributeValue().withS(organizationId.toString()),
            "rightsRetentionStrategy", new AttributeValue().withM(rightsRetentionStrategyArraibuteMap())
        );
    }

    private Map<String, AttributeValue> rightsRetentionStrategyArraibuteMap() {
        return Map.of(
            "type", new AttributeValue().withS("NullRightsRetentionStrategy")
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

    record ChannelClaimContext(URI channelClaimId, UUID channelIdentifier, URI channelId, UUID customerIdentifier,
                               URI customerId, URI organizationId) {

    }
}
