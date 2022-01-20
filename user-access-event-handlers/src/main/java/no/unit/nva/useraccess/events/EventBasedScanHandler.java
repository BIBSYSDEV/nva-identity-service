package no.unit.nva.useraccess.events;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBasedScanHandler
    extends DestinationsEventBridgeEventHandler<ScanDatabaseRequest, Void> {

    public static final Void VOID = null;
    private static final Logger logger = LoggerFactory.getLogger(EventBasedScanHandler.class);
    private final MigrationService migrationService;
    private final AmazonDynamoDB dynamoDbClient;
    private final String USERS_AND_ROLES_TABLE = new Environment().readEnv("USERS_AND_ROLES_TABLE");
    private final EventBridgeClient eventsClient;

    @JacocoGenerated
    public EventBasedScanHandler() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), EventBridgeClient.create(), new EchoMigrationService());
    }

    public EventBasedScanHandler(AmazonDynamoDB dynamoDbClient,
                                 EventBridgeClient eventsClient,
                                 MigrationService migrationService) {
        super(ScanDatabaseRequest.class);
        this.migrationService = migrationService;
        this.dynamoDbClient = dynamoDbClient;
        this.eventsClient = eventsClient;
    }

    @Override
    protected Void processInputPayload(
        ScanDatabaseRequest scanDatabaseRequest,
        AwsEventBridgeEvent<AwsEventBridgeDetail<ScanDatabaseRequest>> event,
        Context context) {

        var scanResult = fetchOnePageOfUsers(scanDatabaseRequest);
        migrateUsers(scanResult.getRetrievedUsers());
        var eventForNextScanRequest = creteEventForNextPageScan(scanDatabaseRequest, scanResult, context);
        eventsClient.putEvents(eventForNextScanRequest);
        logger.info("nextEvent:" + eventForNextScanRequest);
        return VOID;
    }

    private void migrateUsers(List<UserDto> users) {
        var migratedUsers = users
            .stream()
            .map(migrationService::migrateUserDto)
            .collect(Collectors.toList());
        migratedUsers.forEach(user -> logger.info("migratedUser:" + user.toJsonString()));
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

    private UserScanResult fetchOnePageOfUsers(ScanDatabaseRequest scanRequest) {
        var result = scanDynamoDb(scanRequest);
        var startMarkerForNextScan = result.getLastEvaluatedKey();
        var retrievedUsers = parseUsersFromScanResult(result);
        return new UserScanResult(retrievedUsers, startMarkerForNextScan);
    }

    private ScanResult scanDynamoDb(ScanDatabaseRequest scanRequest) {
        var dynamoScanRequest = createrScanDynamoRequest(scanRequest);
        return dynamoDbClient.scan(dynamoScanRequest);
    }

    private List<UserDto> parseUsersFromScanResult(ScanResult result) {
        return result.getItems().stream()
            .map(ItemUtils::toItem)
            .filter(this::databaseEntryIsUser)
            .map(Item::toJSON)
            .map(UserDb::fromJson)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());
    }

    private boolean databaseEntryIsUser(Item item) {
        return item.getString(WithType.TYPE_FIELD).equals(UserDb.TYPE);
    }

    private ScanRequest createrScanDynamoRequest(ScanDatabaseRequest input) {
        return new ScanRequest()
            .withTableName(USERS_AND_ROLES_TABLE)
            .withLimit(input.getPageSize())
            .withExclusiveStartKey(input.getStartMarker());
    }

    private static class UserScanResult {

        private final List<UserDto> retrievedUsers;
        private final Map<String, AttributeValue> startMarkerForNextScan;

        public UserScanResult(List<UserDto> retrievedUsers,
                              Map<String, AttributeValue> startMarkerForNextScan) {
            this.retrievedUsers = retrievedUsers;
            this.startMarkerForNextScan = startMarkerForNextScan;
        }

        public List<UserDto> getRetrievedUsers() {
            return retrievedUsers;
        }

        public Map<String, AttributeValue> getStartMarkerForNextScan() {
            return startMarkerForNextScan;
        }
    }
}
