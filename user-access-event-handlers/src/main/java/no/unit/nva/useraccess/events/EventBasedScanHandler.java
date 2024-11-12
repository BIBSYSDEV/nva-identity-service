package no.unit.nva.useraccess.events;

import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;

import java.util.List;
import java.util.stream.Collectors;

import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.useraccess.events.service.UserMigrationService;
import no.unit.nva.useraccess.events.service.UserMigrationServiceImpl;
import no.unit.nva.useraccessservice.internals.UserScanResult;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBasedScanHandler extends EventHandler<ScanDatabaseRequestV2, Void> {

    public static final Void VOID = null;
    public static final String END_OF_SCAN_MESSAGE = "Last event was processed.";
    private static final Logger logger = LoggerFactory.getLogger(EventBasedScanHandler.class);
    private final UserMigrationService migrationService;
    private final EventBridgeClient eventsClient;
    private final IdentityServiceImpl identityService;

    @JacocoGenerated
    public EventBasedScanHandler() {
        this(DEFAULT_DYNAMO_CLIENT);
    }

    @JacocoGenerated
    public EventBasedScanHandler(DynamoDbClient dynamoDBClient) {
        this(dynamoDBClient, EventsConfig.EVENTS_CLIENT, defaultMigrationService(dynamoDBClient));
    }

    public EventBasedScanHandler(DynamoDbClient dynamoDbClient,
                                 EventBridgeClient eventsClient,
                                 UserMigrationService migrationService) {
        super(ScanDatabaseRequestV2.class);
        this.migrationService = migrationService;
        this.identityService = new IdentityServiceImpl(dynamoDbClient);
        this.eventsClient = eventsClient;
    }

    @JacocoGenerated
    private static UserMigrationServiceImpl defaultMigrationService(
            DynamoDbClient dynamoDBClient) {
        DynamoDBCustomerService customerService = new DynamoDBCustomerService(dynamoDBClient);
        return new UserMigrationServiceImpl(customerService);
    }

    @Override
    protected Void processInput(ScanDatabaseRequestV2 scanDatabaseRequest,
                                AwsEventBridgeEvent<ScanDatabaseRequestV2> event,
                                Context context) {

        var scanResult = identityService.fetchOnePageOfUsers(scanDatabaseRequest);
        var migratedUsers = migrateUsers(scanResult.getRetrievedUsers());
        persistMigratedUsersToDatabase(migratedUsers);
        emitNexScanRequestIfThereAreMoreResults(scanResult, scanDatabaseRequest, context);
        return VOID;
    }

    private List<UserDto> persistMigratedUsersToDatabase(List<UserDto> migratedUsers) {
        var updatedUsers = migratedUsers.stream()
                .map(this::updateUser)
                .map(attempt(identityService::getUser))
                .map(Try::orElseThrow)
                .collect(Collectors.toList());

        updatedUsers.forEach(user -> logger.info("UpdatedUser:" + user.toString()));

        return updatedUsers;
    }

    private UserDto updateUser(UserDto user) {
        try {
            identityService.updateUser(user);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private void emitNexScanRequestIfThereAreMoreResults(UserScanResult scanResult,
                                                         ScanDatabaseRequestV2 inputRequest,
                                                         Context context) {
        if (scanResult.thereAreMoreEntries()) {
            emitNextScanRequest(scanResult, inputRequest, context);
        } else {
            logger.info(END_OF_SCAN_MESSAGE);
        }
    }

    private void emitNextScanRequest(UserScanResult scanResult, ScanDatabaseRequestV2 inputRequest, Context context) {
        var eventForNextScanRequest = creteEventForNextPageScan(inputRequest, scanResult, context);
        eventsClient.putEvents(eventForNextScanRequest);
        logger.info("nextEvent:" + eventForNextScanRequest);
    }

    private List<UserDto> migrateUsers(List<UserDto> users) {
        var migratedUsers = users
                .stream()
                .map(migrationService::migrateUser)
                .collect(Collectors.toList());
        logger.info("Number of users to be migrated:" + migratedUsers.size());
        return migratedUsers;
    }

    private PutEventsRequest creteEventForNextPageScan(ScanDatabaseRequestV2 inputScanRequest,
                                                       UserScanResult scanResult,
                                                       Context context) {
        return attempt(() -> createNextScanRequest(inputScanRequest, scanResult))
                .map(scanRequest -> createNewEventEntry(context, scanRequest))
                .map(eventEntry -> PutEventsRequest.builder().entries(eventEntry).build())
                .orElseThrow();
    }

    private PutEventsRequestEntry createNewEventEntry(Context context, ScanDatabaseRequestV2 scanRequest) {
        return scanRequest.createNewEventEntry(EventsConfig.EVENT_BUS,
                EventsConfig.SCAN_REQUEST_EVENTS_DETAIL_TYPE,
                context.getInvokedFunctionArn());
    }

    private ScanDatabaseRequestV2 createNextScanRequest(ScanDatabaseRequestV2 input, UserScanResult scanResult) {
        return input.newScanDatabaseRequest(scanResult.getStartMarkerForNextScan());
    }
}
