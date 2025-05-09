package no.unit.nva.customer.events.handler;

import java.util.function.Consumer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeServiceClientConfiguration;
import software.amazon.awssdk.services.eventbridge.model.AccessDeniedException;
import software.amazon.awssdk.services.eventbridge.model.ActivateEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.ActivateEventSourceRequest.Builder;
import software.amazon.awssdk.services.eventbridge.model.ActivateEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.CancelReplayRequest;
import software.amazon.awssdk.services.eventbridge.model.CancelReplayResponse;
import software.amazon.awssdk.services.eventbridge.model.ConcurrentModificationException;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.CreateArchiveRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateArchiveResponse;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.CreateEndpointRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateEndpointResponse;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.CreateEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.CreatePartnerEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.CreatePartnerEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.DeactivateEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.DeactivateEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.DeauthorizeConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DeauthorizeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteArchiveRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteArchiveResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteEndpointResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DeletePartnerEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.DeletePartnerEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.DeleteRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.DeleteRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeArchiveRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeArchiveResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventBusResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribePartnerEventSourceRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribePartnerEventSourceResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeReplayRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeReplayResponse;
import software.amazon.awssdk.services.eventbridge.model.DescribeRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.DescribeRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.DisableRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.DisableRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.EnableRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.EnableRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.IllegalStatusException;
import software.amazon.awssdk.services.eventbridge.model.InternalException;
import software.amazon.awssdk.services.eventbridge.model.InvalidEventPatternException;
import software.amazon.awssdk.services.eventbridge.model.InvalidStateException;
import software.amazon.awssdk.services.eventbridge.model.LimitExceededException;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListApiDestinationsResponse;
import software.amazon.awssdk.services.eventbridge.model.ListArchivesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListArchivesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListConnectionsResponse;
import software.amazon.awssdk.services.eventbridge.model.ListEndpointsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEndpointsResponse;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListEventSourcesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventSourcesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListPartnerEventSourceAccountsRequest;
import software.amazon.awssdk.services.eventbridge.model.ListPartnerEventSourceAccountsResponse;
import software.amazon.awssdk.services.eventbridge.model.ListPartnerEventSourcesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListPartnerEventSourcesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListReplaysRequest;
import software.amazon.awssdk.services.eventbridge.model.ListReplaysResponse;
import software.amazon.awssdk.services.eventbridge.model.ListRuleNamesByTargetRequest;
import software.amazon.awssdk.services.eventbridge.model.ListRuleNamesByTargetResponse;
import software.amazon.awssdk.services.eventbridge.model.ListRulesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListRulesResponse;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.ManagedRuleException;
import software.amazon.awssdk.services.eventbridge.model.OperationDisabledException;
import software.amazon.awssdk.services.eventbridge.model.PolicyLengthExceededException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;
import software.amazon.awssdk.services.eventbridge.model.PutPartnerEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutPartnerEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionRequest;
import software.amazon.awssdk.services.eventbridge.model.PutPermissionResponse;
import software.amazon.awssdk.services.eventbridge.model.PutRuleRequest;
import software.amazon.awssdk.services.eventbridge.model.PutRuleResponse;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutTargetsResponse;
import software.amazon.awssdk.services.eventbridge.model.RemovePermissionRequest;
import software.amazon.awssdk.services.eventbridge.model.RemovePermissionResponse;
import software.amazon.awssdk.services.eventbridge.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.eventbridge.model.RemoveTargetsResponse;
import software.amazon.awssdk.services.eventbridge.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.eventbridge.model.ResourceNotFoundException;
import software.amazon.awssdk.services.eventbridge.model.StartReplayRequest;
import software.amazon.awssdk.services.eventbridge.model.StartReplayResponse;
import software.amazon.awssdk.services.eventbridge.model.TagResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.TagResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.TestEventPatternRequest;
import software.amazon.awssdk.services.eventbridge.model.TestEventPatternResponse;
import software.amazon.awssdk.services.eventbridge.model.ThrottlingException;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceRequest;
import software.amazon.awssdk.services.eventbridge.model.UntagResourceResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateApiDestinationResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateArchiveRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateArchiveResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateConnectionResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateEndpointRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateEndpointResponse;
import software.amazon.awssdk.services.eventbridge.model.UpdateEventBusRequest;
import software.amazon.awssdk.services.eventbridge.model.UpdateEventBusResponse;

public class FailingEventBridgeClient implements EventBridgeClient {

    private final boolean nonSuccessfulHttpStatus;

    public FailingEventBridgeClient(boolean nonSuccessfulHttpStatus) {
        this.nonSuccessfulHttpStatus = nonSuccessfulHttpStatus;
    }

    @Override
    public String serviceName() {
        return "";
    }

    @Override
    public void close() {

    }

    @Override
    public ActivateEventSourceResponse activateEventSource(ActivateEventSourceRequest activateEventSourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InvalidStateException, InternalException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ActivateEventSourceResponse activateEventSource(Consumer<Builder> activateEventSourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InvalidStateException, InternalException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CancelReplayResponse cancelReplay(CancelReplayRequest cancelReplayRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, IllegalStatusException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CancelReplayResponse cancelReplay(Consumer<CancelReplayRequest.Builder> cancelReplayRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, IllegalStatusException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateApiDestinationResponse createApiDestination(CreateApiDestinationRequest createApiDestinationRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, LimitExceededException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateApiDestinationResponse createApiDestination(
        Consumer<CreateApiDestinationRequest.Builder> createApiDestinationRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, LimitExceededException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateArchiveResponse createArchive(CreateArchiveRequest createArchiveRequest)
        throws ConcurrentModificationException, ResourceAlreadyExistsException, ResourceNotFoundException,
               InternalException, LimitExceededException, InvalidEventPatternException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateArchiveResponse createArchive(Consumer<CreateArchiveRequest.Builder> createArchiveRequest)
        throws ConcurrentModificationException, ResourceAlreadyExistsException, ResourceNotFoundException,
               InternalException, LimitExceededException, InvalidEventPatternException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateConnectionResponse createConnection(CreateConnectionRequest createConnectionRequest)
        throws ResourceAlreadyExistsException, LimitExceededException, ResourceNotFoundException, InternalException,
               AccessDeniedException, ThrottlingException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateConnectionResponse createConnection(Consumer<CreateConnectionRequest.Builder> createConnectionRequest)
        throws ResourceAlreadyExistsException, LimitExceededException, ResourceNotFoundException, InternalException,
               AccessDeniedException, ThrottlingException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateEndpointResponse createEndpoint(CreateEndpointRequest createEndpointRequest)
        throws ResourceAlreadyExistsException, LimitExceededException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateEndpointResponse createEndpoint(Consumer<CreateEndpointRequest.Builder> createEndpointRequest)
        throws ResourceAlreadyExistsException, LimitExceededException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateEventBusResponse createEventBus(CreateEventBusRequest createEventBusRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, InvalidStateException, InternalException,
               ConcurrentModificationException, LimitExceededException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreateEventBusResponse createEventBus(Consumer<CreateEventBusRequest.Builder> createEventBusRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, InvalidStateException, InternalException,
               ConcurrentModificationException, LimitExceededException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreatePartnerEventSourceResponse createPartnerEventSource(
        CreatePartnerEventSourceRequest createPartnerEventSourceRequest)
        throws ResourceAlreadyExistsException, InternalException, ConcurrentModificationException,
               LimitExceededException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public CreatePartnerEventSourceResponse createPartnerEventSource(
        Consumer<CreatePartnerEventSourceRequest.Builder> createPartnerEventSourceRequest)
        throws ResourceAlreadyExistsException, InternalException, ConcurrentModificationException,
               LimitExceededException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeactivateEventSourceResponse deactivateEventSource(
        DeactivateEventSourceRequest deactivateEventSourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InvalidStateException, InternalException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeactivateEventSourceResponse deactivateEventSource(
        Consumer<DeactivateEventSourceRequest.Builder> deactivateEventSourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InvalidStateException, InternalException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeauthorizeConnectionResponse deauthorizeConnection(
        DeauthorizeConnectionRequest deauthorizeConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeauthorizeConnectionResponse deauthorizeConnection(
        Consumer<DeauthorizeConnectionRequest.Builder> deauthorizeConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteApiDestinationResponse deleteApiDestination(DeleteApiDestinationRequest deleteApiDestinationRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteApiDestinationResponse deleteApiDestination(
        Consumer<DeleteApiDestinationRequest.Builder> deleteApiDestinationRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteArchiveResponse deleteArchive(DeleteArchiveRequest deleteArchiveRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteArchiveResponse deleteArchive(Consumer<DeleteArchiveRequest.Builder> deleteArchiveRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteConnectionResponse deleteConnection(DeleteConnectionRequest deleteConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteConnectionResponse deleteConnection(Consumer<DeleteConnectionRequest.Builder> deleteConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteEndpointResponse deleteEndpoint(DeleteEndpointRequest deleteEndpointRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteEndpointResponse deleteEndpoint(Consumer<DeleteEndpointRequest.Builder> deleteEndpointRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteEventBusResponse deleteEventBus(DeleteEventBusRequest deleteEventBusRequest)
        throws InternalException, ConcurrentModificationException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteEventBusResponse deleteEventBus(Consumer<DeleteEventBusRequest.Builder> deleteEventBusRequest)
        throws InternalException, ConcurrentModificationException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeletePartnerEventSourceResponse deletePartnerEventSource(
        DeletePartnerEventSourceRequest deletePartnerEventSourceRequest)
        throws InternalException, ConcurrentModificationException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeletePartnerEventSourceResponse deletePartnerEventSource(
        Consumer<DeletePartnerEventSourceRequest.Builder> deletePartnerEventSourceRequest)
        throws InternalException, ConcurrentModificationException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteRuleResponse deleteRule(DeleteRuleRequest deleteRuleRequest)
        throws ConcurrentModificationException, ManagedRuleException, InternalException, ResourceNotFoundException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DeleteRuleResponse deleteRule(Consumer<DeleteRuleRequest.Builder> deleteRuleRequest)
        throws ConcurrentModificationException, ManagedRuleException, InternalException, ResourceNotFoundException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeApiDestinationResponse describeApiDestination(
        DescribeApiDestinationRequest describeApiDestinationRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeApiDestinationResponse describeApiDestination(
        Consumer<DescribeApiDestinationRequest.Builder> describeApiDestinationRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeArchiveResponse describeArchive(DescribeArchiveRequest describeArchiveRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeArchiveResponse describeArchive(Consumer<DescribeArchiveRequest.Builder> describeArchiveRequest)
        throws ResourceAlreadyExistsException, ResourceNotFoundException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeConnectionResponse describeConnection(DescribeConnectionRequest describeConnectionRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeConnectionResponse describeConnection(
        Consumer<DescribeConnectionRequest.Builder> describeConnectionRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEndpointResponse describeEndpoint(DescribeEndpointRequest describeEndpointRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEndpointResponse describeEndpoint(Consumer<DescribeEndpointRequest.Builder> describeEndpointRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEventBusResponse describeEventBus(DescribeEventBusRequest describeEventBusRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEventBusResponse describeEventBus(Consumer<DescribeEventBusRequest.Builder> describeEventBusRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEventSourceResponse describeEventSource(DescribeEventSourceRequest describeEventSourceRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeEventSourceResponse describeEventSource(
        Consumer<DescribeEventSourceRequest.Builder> describeEventSourceRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribePartnerEventSourceResponse describePartnerEventSource(
        DescribePartnerEventSourceRequest describePartnerEventSourceRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribePartnerEventSourceResponse describePartnerEventSource(
        Consumer<DescribePartnerEventSourceRequest.Builder> describePartnerEventSourceRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeReplayResponse describeReplay(DescribeReplayRequest describeReplayRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeReplayResponse describeReplay(Consumer<DescribeReplayRequest.Builder> describeReplayRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeRuleResponse describeRule(DescribeRuleRequest describeRuleRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DescribeRuleResponse describeRule(Consumer<DescribeRuleRequest.Builder> describeRuleRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DisableRuleResponse disableRule(DisableRuleRequest disableRuleRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public DisableRuleResponse disableRule(Consumer<DisableRuleRequest.Builder> disableRuleRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public EnableRuleResponse enableRule(EnableRuleRequest enableRuleRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public EnableRuleResponse enableRule(Consumer<EnableRuleRequest.Builder> enableRuleRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListApiDestinationsResponse listApiDestinations(ListApiDestinationsRequest listApiDestinationsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListApiDestinationsResponse listApiDestinations(
        Consumer<ListApiDestinationsRequest.Builder> listApiDestinationsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListArchivesResponse listArchives(ListArchivesRequest listArchivesRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListArchivesResponse listArchives(Consumer<ListArchivesRequest.Builder> listArchivesRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListConnectionsResponse listConnections(ListConnectionsRequest listConnectionsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListConnectionsResponse listConnections(Consumer<ListConnectionsRequest.Builder> listConnectionsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEndpointsResponse listEndpoints(ListEndpointsRequest listEndpointsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEndpointsResponse listEndpoints(Consumer<ListEndpointsRequest.Builder> listEndpointsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEventBusesResponse listEventBuses(ListEventBusesRequest listEventBusesRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEventBusesResponse listEventBuses(Consumer<ListEventBusesRequest.Builder> listEventBusesRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEventSourcesResponse listEventSources(ListEventSourcesRequest listEventSourcesRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListEventSourcesResponse listEventSources(Consumer<ListEventSourcesRequest.Builder> listEventSourcesRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListPartnerEventSourceAccountsResponse listPartnerEventSourceAccounts(
        ListPartnerEventSourceAccountsRequest listPartnerEventSourceAccountsRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListPartnerEventSourceAccountsResponse listPartnerEventSourceAccounts(
        Consumer<ListPartnerEventSourceAccountsRequest.Builder> listPartnerEventSourceAccountsRequest)
        throws ResourceNotFoundException, InternalException, OperationDisabledException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListPartnerEventSourcesResponse listPartnerEventSources(
        ListPartnerEventSourcesRequest listPartnerEventSourcesRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListPartnerEventSourcesResponse listPartnerEventSources(
        Consumer<ListPartnerEventSourcesRequest.Builder> listPartnerEventSourcesRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListReplaysResponse listReplays(ListReplaysRequest listReplaysRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListReplaysResponse listReplays(Consumer<ListReplaysRequest.Builder> listReplaysRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListRuleNamesByTargetResponse listRuleNamesByTarget(
        ListRuleNamesByTargetRequest listRuleNamesByTargetRequest)
        throws InternalException, ResourceNotFoundException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListRuleNamesByTargetResponse listRuleNamesByTarget(
        Consumer<ListRuleNamesByTargetRequest.Builder> listRuleNamesByTargetRequest)
        throws InternalException, ResourceNotFoundException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListRulesResponse listRules(ListRulesRequest listRulesRequest)
        throws InternalException, ResourceNotFoundException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListRulesResponse listRules(Consumer<ListRulesRequest.Builder> listRulesRequest)
        throws InternalException, ResourceNotFoundException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListTagsForResourceResponse listTagsForResource(ListTagsForResourceRequest listTagsForResourceRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListTagsForResourceResponse listTagsForResource(
        Consumer<ListTagsForResourceRequest.Builder> listTagsForResourceRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListTargetsByRuleResponse listTargetsByRule(ListTargetsByRuleRequest listTargetsByRuleRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ListTargetsByRuleResponse listTargetsByRule(
        Consumer<ListTargetsByRuleRequest.Builder> listTargetsByRuleRequest)
        throws ResourceNotFoundException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {

        if (nonSuccessfulHttpStatus) {
            return (PutEventsResponse) PutEventsResponse.builder()
                                           .sdkHttpResponse(SdkHttpResponse.builder()
                                                                .statusCode(400)
                                                                .build())
                                           .build();
        } else {
            return (PutEventsResponse) PutEventsResponse.builder()
                                           .entries(PutEventsResultEntry.builder()
                                                        .errorCode("InvalidParameterValue")
                                                        .errorMessage("InvalidParameterValue")
                                                        .build())
                                           .failedEntryCount(1)
                                           .sdkHttpResponse(SdkHttpResponse.builder()
                                                                .statusCode(200)
                                                                .build())
                                           .build();
        }
    }

    @Override
    public PutEventsResponse putEvents(Consumer<PutEventsRequest.Builder> putEventsRequest)
        throws InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutPartnerEventsResponse putPartnerEvents(PutPartnerEventsRequest putPartnerEventsRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutPartnerEventsResponse putPartnerEvents(Consumer<PutPartnerEventsRequest.Builder> putPartnerEventsRequest)
        throws InternalException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutPermissionResponse putPermission(PutPermissionRequest putPermissionRequest)
        throws ResourceNotFoundException, PolicyLengthExceededException, InternalException,
               ConcurrentModificationException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutPermissionResponse putPermission(Consumer<PutPermissionRequest.Builder> putPermissionRequest)
        throws ResourceNotFoundException, PolicyLengthExceededException, InternalException,
               ConcurrentModificationException, OperationDisabledException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutRuleResponse putRule(PutRuleRequest putRuleRequest)
        throws InvalidEventPatternException, LimitExceededException, ConcurrentModificationException,
               ManagedRuleException, InternalException, ResourceNotFoundException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutRuleResponse putRule(Consumer<PutRuleRequest.Builder> putRuleRequest)
        throws InvalidEventPatternException, LimitExceededException, ConcurrentModificationException,
               ManagedRuleException, InternalException, ResourceNotFoundException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutTargetsResponse putTargets(PutTargetsRequest putTargetsRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, LimitExceededException, ManagedRuleException,
               InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public PutTargetsResponse putTargets(Consumer<PutTargetsRequest.Builder> putTargetsRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, LimitExceededException, ManagedRuleException,
               InternalException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RemovePermissionResponse removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RemoveTargetsResponse removeTargets(RemoveTargetsRequest removeTargetsRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public RemoveTargetsResponse removeTargets(Consumer<RemoveTargetsRequest.Builder> removeTargetsRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, ManagedRuleException, InternalException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public StartReplayResponse startReplay(StartReplayRequest startReplayRequest)
        throws ResourceNotFoundException, ResourceAlreadyExistsException, InvalidEventPatternException,
               LimitExceededException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public StartReplayResponse startReplay(Consumer<StartReplayRequest.Builder> startReplayRequest)
        throws ResourceNotFoundException, ResourceAlreadyExistsException, InvalidEventPatternException,
               LimitExceededException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InternalException, ManagedRuleException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TagResourceResponse tagResource(Consumer<TagResourceRequest.Builder> tagResourceRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InternalException, ManagedRuleException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TestEventPatternResponse testEventPattern(TestEventPatternRequest testEventPatternRequest)
        throws InvalidEventPatternException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TestEventPatternResponse testEventPattern(Consumer<TestEventPatternRequest.Builder> testEventPatternRequest)
        throws InvalidEventPatternException, InternalException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UntagResourceResponse untagResource(UntagResourceRequest untagResourceRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException, ManagedRuleException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UntagResourceResponse untagResource(Consumer<UntagResourceRequest.Builder> untagResourceRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException, ManagedRuleException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateApiDestinationResponse updateApiDestination(UpdateApiDestinationRequest updateApiDestinationRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateApiDestinationResponse updateApiDestination(
        Consumer<UpdateApiDestinationRequest.Builder> updateApiDestinationRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateArchiveResponse updateArchive(UpdateArchiveRequest updateArchiveRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               InvalidEventPatternException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateArchiveResponse updateArchive(Consumer<UpdateArchiveRequest.Builder> updateArchiveRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               InvalidEventPatternException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateConnectionResponse updateConnection(UpdateConnectionRequest updateConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               AccessDeniedException, ThrottlingException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateConnectionResponse updateConnection(Consumer<UpdateConnectionRequest.Builder> updateConnectionRequest)
        throws ConcurrentModificationException, ResourceNotFoundException, InternalException, LimitExceededException,
               AccessDeniedException, ThrottlingException, AwsServiceException, SdkClientException,
               EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateEndpointResponse updateEndpoint(UpdateEndpointRequest updateEndpointRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateEndpointResponse updateEndpoint(Consumer<UpdateEndpointRequest.Builder> updateEndpointRequest)
        throws ResourceNotFoundException, ConcurrentModificationException, InternalException, AwsServiceException,
               SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateEventBusResponse updateEventBus(UpdateEventBusRequest updateEventBusRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public UpdateEventBusResponse updateEventBus(Consumer<UpdateEventBusRequest.Builder> updateEventBusRequest)
        throws ResourceNotFoundException, InternalException, ConcurrentModificationException,
               OperationDisabledException, AwsServiceException, SdkClientException, EventBridgeException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public EventBridgeServiceClientConfiguration serviceClientConfiguration() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
