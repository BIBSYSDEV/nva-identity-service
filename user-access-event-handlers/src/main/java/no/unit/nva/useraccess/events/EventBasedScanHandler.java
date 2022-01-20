package no.unit.nva.useraccess.events;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.useraccessmanagement.internals.UserScanResult;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBasedScanHandler extends EventHandler<ScanDatabaseRequest, Void> {

    public static final Void VOID = null;
    private static final Logger logger = LoggerFactory.getLogger(EventBasedScanHandler.class);
    private final MigrationService migrationService;
    private final EventBridgeClient eventsClient;
    private final IdentityServiceImpl identityService;

    @JacocoGenerated
    public EventBasedScanHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), EventBridgeClient.create(), new EchoMigrationService());
    }

    public EventBasedScanHandler(AmazonDynamoDB dynamoDbClient,
                                 EventBridgeClient eventsClient,
                                 MigrationService migrationService) {
        super(ScanDatabaseRequest.class);
        this.migrationService = migrationService;
        this.identityService = new IdentityServiceImpl(dynamoDbClient);
        this.eventsClient = eventsClient;
    }

    @Override
    protected Void processInput(ScanDatabaseRequest scanDatabaseRequest,
                                AwsEventBridgeEvent<ScanDatabaseRequest> event,
                                Context context) {

        var scanResult = identityService.fetchOnePageOfUsers(scanDatabaseRequest);
        migrateUsers(scanResult.getRetrievedUsers());
        sendNextIfThereAreMoreResults(scanResult, scanDatabaseRequest, context);
        return VOID;
    }

    private void sendNextIfThereAreMoreResults(UserScanResult scanResult,
                                               ScanDatabaseRequest inputRequest,
                                               Context context) {
        if (scanResult.thereAreMoreEntries()) {
            emitNextScanRequest(scanResult, inputRequest, context);
        } else {
            logger.info("Last event was processed.");
        }
    }

    private void emitNextScanRequest(UserScanResult scanResult, ScanDatabaseRequest inputRequest, Context context) {
        var eventForNextScanRequest = creteEventForNextPageScan(inputRequest, scanResult, context);
        eventsClient.putEvents(eventForNextScanRequest);
        logger.info("nextEvent:" + eventForNextScanRequest);
    }

    private List<UserDto> migrateUsers(List<UserDto> users) {
        return users
            .stream()
            .map(migrationService::migrateUserDto)
            .peek(user -> logger.info("migratedUser:" + user.toJsonString()))
            .collect(Collectors.toList());
    }

    private PutEventsRequest creteEventForNextPageScan(ScanDatabaseRequest inputScanRequest,
                                                       UserScanResult scanResult,
                                                       Context context) {
        return attempt(() -> createNextScanRequest(inputScanRequest, scanResult))
            .map(scanRequest -> createNewEventEntry(context, scanRequest))
            .map(eventEntry -> PutEventsRequest.builder().entries(eventEntry).build())
            .orElseThrow();
    }

    private PutEventsRequestEntry createNewEventEntry(Context context, ScanDatabaseRequest scanRequest) {
        return scanRequest.createNewEventEntry(EventsConfig.EVENT_BUS,
                                               EventsConfig.SCAN_REQUEST_EVENTS_DETAIL_TYPE,
                                               context.getInvokedFunctionArn());
    }

    private ScanDatabaseRequest createNextScanRequest(ScanDatabaseRequest input, UserScanResult scanResult) {
        return input.newScanDatabaseRequest(scanResult.getStartMarkerForNextScan());
    }
}
