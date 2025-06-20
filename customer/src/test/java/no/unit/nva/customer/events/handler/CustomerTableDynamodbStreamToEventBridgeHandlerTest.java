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
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.events.emitter.EventEmitterException;
import no.unit.nva.customer.events.model.ChannelClaim;
import no.unit.nva.customer.events.model.ChannelClaim.Constraints;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import no.unit.nva.customer.model.PublicationInstanceTypes;
import no.unit.nva.customer.model.channelclaim.ChannelConstraintPolicy;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
    @Disabled
    void shouldFail() throws JsonProcessingException {
        var jsonBody = """
            {
                "eventID": "55621683b48de2091b83e678ae88bc05",
                "eventName": "MODIFY",
                "eventVersion": "1.1",
                "eventSource": "aws:dynamodb",
                "awsRegion": "eu-west-1",
                "dynamodb": {
                    "approximateCreationDateTime": "2025-06-19T11:25:29.000+00:00",
                    "keys": {
                        "identifier": {
                            "s": "a228aba6-932b-4f53-b2de-31ad8daf9f8d"
                        }
                    },
                    "newImage": {
                        "identifier": {
                            "s": "a228aba6-932b-4f53-b2de-31ad8daf9f8d"
                        },
                        "rightsRetentionStrategy": {
                            "m": {
                                "type": {
                                    "s": "NullRightsRetentionStrategy"
                                }
                            }
                        },
                        "displayName": {
                            "s": "Universitetet i Bergen"
                        },
                        "cristinId": {
                            "s": "https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0"
                        },
                        "rorId": {
                            "s": "https://ror.org/abcd0123ef"
                        },
                        "generalSupportEnabled": {
                            "bool": true
                        },
                        "type": {
                            "s": "Customer"
                        },
                        "feideOrganizationDomain": {
                            "s": "uib.no"
                        },
                        "createdDate": {
                            "s": "2023-07-07T07:35:33.765827Z"
                        },
                        "customerOf": {
                            "s": "nva.unit.no"
                        },
                        "nviInstitution": {
                            "bool": true
                        },
                        "modifiedDate": {
                            "s": "2025-06-19T11:25:29.715178927Z"
                        },
                        "name": {
                            "s": "Universitetet i Bergen"
                        },
                        "allowFileUploadForTypes": {
                            "ss": [
                                "ACADEMIC_ARTICLE",
                                "ACADEMIC_CHAPTER",
                                "ACADEMIC_LITERATURE_REVIEW",
                                "ACADEMIC_MONOGRAPH",
                                "ARCHITECTURE",
                                "ARTISTIC_DESIGN",
                                "BOOK_ANTHOLOGY",
                                "CASE_REPORT",
                                "CHAPTER_CONFERENCE_ABSTRACT",
                                "CHAPTER_IN_REPORT",
                                "CONFERENCE_ABSTRACT",
                                "CONFERENCE_LECTURE",
                                "CONFERENCE_POSTER",
                                "CONFERENCE_REPORT",
                                "DATA_MANAGEMENT_PLAN",
                                "DATA_SET",
                                "DEGREE_BACHELOR",
                                "DEGREE_LICENTIATE",
                                "DEGREE_MASTER",
                                "DEGREE_PHD",
                                "ENCYCLOPEDIA",
                                "ENCYCLOPEDIA_CHAPTER",
                                "EXHIBITION_CATALOG",
                                "EXHIBITION_CATALOG_CHAPTER",
                                "EXHIBITION_PRODUCTION",
                                "INTRODUCTION",
                                "JOURNAL_CORRIGENDUM",
                                "JOURNAL_ISSUE",
                                "JOURNAL_LEADER",
                                "JOURNAL_LETTER",
                                "JOURNAL_REVIEW",
                                "LECTURE",
                                "LITERARY_ARTS",
                                "MAP",
                                "MEDIA_BLOG_POST",
                                "MEDIA_FEATURE_ARTICLE",
                                "MEDIA_INTERVIEW",
                                "MEDIA_PARTICIPATION_IN_RADIO_OR_TV",
                                "MEDIA_PODCAST",
                                "MEDIA_READER_OPINION",
                                "MOVING_PICTURE",
                                "MUSIC_PERFORMANCE",
                                "NON_FICTION_CHAPTER",
                                "NON_FICTION_MONOGRAPH",
                                "OTHER_PRESENTATION",
                                "OTHER_STUDENT_WORK",
                                "PERFORMING_ARTS",
                                "POPULAR_SCIENCE_ARTICLE",
                                "POPULAR_SCIENCE_CHAPTER",
                                "POPULAR_SCIENCE_MONOGRAPH",
                                "PROFESSIONAL_ARTICLE",
                                "REPORT_BASIC",
                                "REPORT_BOOK_OF_ABSTRACT",
                                "REPORT_RESEARCH",
                                "REPORT_WORKING_PAPER",
                                "REPOST_POLICY",
                                "STUDY_PROTOCOL",
                                "TEXTBOOK",
                                "TEXTBOOK_CHAPTER",
                                "VISUAL_ARTS"
                            ]
                        },
                        "publicationWorkflow": {
                            "s": "REGISTRATOR_PUBLISHES_METADATA_ONLY"
                        },
                        "channelClaims": {
                            "l": [
                                {
                                    "m": {
                                        "channel": {
                                            "s": "https://api.dev.nva.aws.unit.no/publication-channels-v2/publisher/CBCE38D7-C6C6-4CE9-BCED-D64610033E9B"
                                        },
                                        "constraint": {
                                            "m": {
                                                "scope": {
                                                    "l": [
                                                        {
                                                            "s": "DEGREE_BACHELOR"
                                                        },
                                                        {
                                                            "s": "DEGREE_MASTER"
                                                        },
                                                        {
                                                            "s": "DEGREE_PHD"
                                                        },
                                                        {
                                                            "s": "ARTISTIC_DEGREE_PHD"
                                                        },
                                                        {
                                                            "s": "DEGREE_LICENTIATE"
                                                        },
                                                        {
                                                            "s": "OTHER_STUDENT_WORK"
                                                        }
                                                    ]
                                                },
                                                "publishingPolicy": {
                                                    "s": "EVERYONE"
                                                },
                                                "editingPolicy": {
                                                    "s": "OWNER_ONLY"
                                                }
                                            }
                                        }
                                    }
                                }
                            ]
                        },
                        "serviceCenter": {
                            "m": {
                                "name": {
                                    "null": true
                                },
                                "uri": {
                                    "null": true
                                }
                            }
                        },
                        "shortName": {
                            "s": "UIB"
                        },
                        "doiAgent": {
                            "m": {}
                        },
                        "sector": {
                            "s": "UHI"
                        },
                        "rboInstitution": {
                            "bool": false
                        }
                    },
                    "oldImage": {
                        "identifier": {
                            "s": "a228aba6-932b-4f53-b2de-31ad8daf9f8d"
                        },
                        "rightsRetentionStrategy": {
                            "m": {
                                "type": {
                                    "s": "NullRightsRetentionStrategy"
                                }
                            }
                        },
                        "displayName": {
                            "s": "Universitetet i Bergen"
                        },
                        "cristinId": {
                            "s": "https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0"
                        },
                        "rorId": {
                            "s": ""
                        },
                        "generalSupportEnabled": {
                            "bool": true
                        },
                        "type": {
                            "s": "Customer"
                        },
                        "feideOrganizationDomain": {
                            "s": "uib.no"
                        },
                        "createdDate": {
                            "s": "2023-07-07T07:35:33.765827Z"
                        },
                        "customerOf": {
                            "s": "nva.unit.no"
                        },
                        "nviInstitution": {
                            "bool": true
                        },
                        "modifiedDate": {
                            "s": "2025-06-19T10:38:52.785521737Z"
                        },
                        "name": {
                            "s": "Universitetet i Bergen"
                        },
                        "allowFileUploadForTypes": {
                            "ss": [
                                "ACADEMIC_ARTICLE",
                                "ACADEMIC_CHAPTER",
                                "ACADEMIC_LITERATURE_REVIEW",
                                "ACADEMIC_MONOGRAPH",
                                "ARCHITECTURE",
                                "ARTISTIC_DESIGN",
                                "BOOK_ANTHOLOGY",
                                "CASE_REPORT",
                                "CHAPTER_CONFERENCE_ABSTRACT",
                                "CHAPTER_IN_REPORT",
                                "CONFERENCE_ABSTRACT",
                                "CONFERENCE_LECTURE",
                                "CONFERENCE_POSTER",
                                "CONFERENCE_REPORT",
                                "DATA_MANAGEMENT_PLAN",
                                "DATA_SET",
                                "DEGREE_BACHELOR",
                                "DEGREE_LICENTIATE",
                                "DEGREE_MASTER",
                                "DEGREE_PHD",
                                "ENCYCLOPEDIA",
                                "ENCYCLOPEDIA_CHAPTER",
                                "EXHIBITION_CATALOG",
                                "EXHIBITION_CATALOG_CHAPTER",
                                "EXHIBITION_PRODUCTION",
                                "INTRODUCTION",
                                "JOURNAL_CORRIGENDUM",
                                "JOURNAL_ISSUE",
                                "JOURNAL_LEADER",
                                "JOURNAL_LETTER",
                                "JOURNAL_REVIEW",
                                "LECTURE",
                                "LITERARY_ARTS",
                                "MAP",
                                "MEDIA_BLOG_POST",
                                "MEDIA_FEATURE_ARTICLE",
                                "MEDIA_INTERVIEW",
                                "MEDIA_PARTICIPATION_IN_RADIO_OR_TV",
                                "MEDIA_PODCAST",
                                "MEDIA_READER_OPINION",
                                "MOVING_PICTURE",
                                "MUSIC_PERFORMANCE",
                                "NON_FICTION_CHAPTER",
                                "NON_FICTION_MONOGRAPH",
                                "OTHER_PRESENTATION",
                                "OTHER_STUDENT_WORK",
                                "PERFORMING_ARTS",
                                "POPULAR_SCIENCE_ARTICLE",
                                "POPULAR_SCIENCE_CHAPTER",
                                "POPULAR_SCIENCE_MONOGRAPH",
                                "PROFESSIONAL_ARTICLE",
                                "REPORT_BASIC",
                                "REPORT_BOOK_OF_ABSTRACT",
                                "REPORT_RESEARCH",
                                "REPORT_WORKING_PAPER",
                                "REPOST_POLICY",
                                "STUDY_PROTOCOL",
                                "TEXTBOOK",
                                "TEXTBOOK_CHAPTER",
                                "VISUAL_ARTS"
                            ]
                        },
                        "publicationWorkflow": {
                            "s": "REGISTRATOR_PUBLISHES_METADATA_ONLY"
                        },
                        "channelClaims": {
                            "l": [
                                {
                                    "m": {
                                        "channel": {
                                            "s": "https://api.dev.nva.aws.unit.no/publication-channels-v2/publisher/CBCE38D7-C6C6-4CE9-BCED-D64610033E9B"
                                        },
                                        "constraint": {
                                            "m": {
                                                "scope": {
                                                    "l": [
                                                        {
                                                            "s": "DEGREE_BACHELOR"
                                                        },
                                                        {
                                                            "s": "DEGREE_MASTER"
                                                        },
                                                        {
                                                            "s": "DEGREE_PHD"
                                                        },
                                                        {
                                                            "s": "ARTISTIC_DEGREE_PHD"
                                                        },
                                                        {
                                                            "s": "DEGREE_LICENTIATE"
                                                        },
                                                        {
                                                            "s": "OTHER_STUDENT_WORK"
                                                        }
                                                    ]
                                                },
                                                "publishingPolicy": {
                                                    "s": "EVERYONE"
                                                },
                                                "editingPolicy": {
                                                    "s": "OWNER_ONLY"
                                                }
                                            }
                                        }
                                    }
                                }
                            ]
                        },
                        "serviceCenter": {
                            "m": {
                                "name": {
                                    "null": true
                                },
                                "uri": {
                                    "null": true
                                }
                            }
                        },
                        "shortName": {
                            "s": "UIB"
                        },
                        "doiAgent": {
                            "m": {}
                        },
                        "sector": {
                            "s": "UHI"
                        },
                        "rboInstitution": {
                            "bool": false
                        }
                    },
                    "sequenceNumber": "4981469800001502076366178526",
                    "sizeBytes": 3800,
                    "streamViewType": "NEW_AND_OLD_IMAGES"
                },
                "eventSourceARN": "arn:aws:dynamodb:eu-west-1:884807050265:table/nva-customers-master-pipelines-NvaIdentityService-WLJCBMUDMRYZ-nva-identity-service/stream/2025-05-09T11:18:51.458"
            }
            """;
        var foo = JsonUtils.dtoObjectMapper.readValue(jsonBody, DynamodbStreamRecord.class);
        var event = new DynamodbEvent();
        event.setRecords(Collections.singletonList(foo));

        try {
            handler.handleRequest(event, context);
        } catch (Exception e) {
            // no-op
        };

        assertDoesNotThrow(() -> handler.handleRequest(event, context));
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
