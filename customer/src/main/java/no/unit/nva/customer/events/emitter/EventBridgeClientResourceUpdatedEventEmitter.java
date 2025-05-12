package no.unit.nva.customer.events.emitter;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.customer.events.model.IdentifiedResource;
import no.unit.nva.customer.events.model.ResourceUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class EventBridgeClientResourceUpdatedEventEmitter implements ResourceUpdatedEventEmitter {

    private static final Logger logger = LoggerFactory.getLogger(EventBridgeClientResourceUpdatedEventEmitter.class);
    private static final String EVENT_SOURCE = "NVA.IdentityService.Customer";

    private final EventBridgeClient eventBridgeClient;
    private final String eventBusName;
    private final Context context;

    public EventBridgeClientResourceUpdatedEventEmitter(final EventBridgeClient eventBridgeClient,
                                                        final String eventBusName,
                                                        final Context context) {
        this.eventBridgeClient = eventBridgeClient;
        this.eventBusName = eventBusName;
        this.context = context;
    }

    @Override
    public <T extends IdentifiedResource> void emitEvents(final List<ResourceUpdateEvent<T>> resourceUpdateEvents,
                                                          final String detailType) {
        var entries = resourceUpdateEvents.stream()
                          .map(event -> toPutEventsRequestEntry(event, detailType, context))
                          .toList();
        var putEventsRequest = PutEventsRequest.builder()
                                   .entries(entries)
                                   .build();
        var response = eventBridgeClient.putEvents(putEventsRequest);

        reportResult(response);
    }

    private void reportResult(PutEventsResponse response) {
        Optional.of(response)
            .map(PutEventsResponse::sdkHttpResponse)
            .ifPresent(this::logAndThrowHttpErrorIfNeeded);

        var failedEntryCount = response.failedEntryCount();
        if (failedEntryCount > 0) {
            response.entries().stream()
                .filter(entry -> nonNull(entry.errorCode()))
                .forEach(entry -> logger.error("Failed to put event. Error code: {}, Error message: {}",
                                               entry.errorCode(), entry.errorMessage()));
            throw new EventEmitterException(String.format("Failed to put %d events to EventBridge", failedEntryCount));
        }

        logger.info("Successfully put {} events to EventBridge", response.entries().size());
    }

    private void logAndThrowHttpErrorIfNeeded(SdkHttpResponse sdkHttpResponse) {
        if (sdkHttpResponse.statusCode() != HTTP_OK) {
            logger.error("Failed to put events to EventBridge. Status code: {}",
                         sdkHttpResponse.statusCode());
            throw new EventEmitterException("Failed to put events to EventBridge");
        }
    }

    private PutEventsRequestEntry toPutEventsRequestEntry(final ResourceUpdateEvent<?> resourceUpdateEvent,
                                                          String detailType, Context context) {
        var detail = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(resourceUpdateEvent)).orElseThrow();
        return PutEventsRequestEntry.builder()
                   .eventBusName(eventBusName)
                   .time(Instant.now())
                   .source(EVENT_SOURCE)
                   .detailType(detailType)
                   .resources(context.getInvokedFunctionArn())
                   .detail(detail)
                   .build();
    }
}
