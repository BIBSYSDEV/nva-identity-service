package no.unit.nva.useraccess.events;

import static no.unit.nva.database.IdentityService.USERS_AND_ROLES_TABLE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.models.ScanDatabaseRequestV2;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import no.unit.nva.useraccess.events.service.EchoMigrationService;
import no.unit.nva.useraccess.events.service.UserMigrationService;
import no.unit.nva.useraccessmanagement.dao.UserDao;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class EventBasedScanHandlerTest extends DatabaseAccessor {

    public static final Context CONTEXT = mock(Context.class);
    public static final Map<String, String> NO_START_MARKER = null;
    private ByteArrayOutputStream outputStream;
    private EventBasedScanHandler handler;

    private IdentityServiceImpl identityService;
    private EchoMigrationService migrationService;
    private FakeEventBridgeClient eventClient;
    private DynamoDbEnhancedClient enhancedClient;

    @BeforeEach
    public void init() {
        super.initializeTestDatabase();
        this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(localDynamo).build();
        this.identityService = new IdentityServiceImpl(localDynamo);
        this.eventClient = new FakeEventBridgeClient();
        outputStream = new ByteArrayOutputStream();
        migrationService = new EchoMigrationService();
        handler = new EventBasedScanHandler(localDynamo, eventClient, migrationService);
    }

    @ParameterizedTest(name = "should send message to Event Bridge for next scan containing the key of the last "
                              + "scanned object.Page size:{0}")
    @ValueSource(ints = {1, 5, 10, 50})
    void shouldSendEventForNextScanContainingTheKeyOfLastScannedObjectAsNextScanStartMarker(int pageSize) {
        final var insertedUsers = insertRandomUsers(2 * pageSize);
        var inputEvent = sampleEventWithoutStartingPointer(pageSize);
        handler.handleRequest(inputEvent, outputStream, CONTEXT);
        var emittedScanRequest = extractScanRequestFromEmittedMessage();

        var expectedTableEntry = calculateLastScannedUser(pageSize, insertedUsers);
        var actualTableEntry = fetchLastScannedUserUsingTheEmittedEvent(emittedScanRequest);
        assertThat(actualTableEntry, is(equalTo(expectedTableEntry)));
    }

    @ParameterizedTest(name = "should scan all users in database and not enter an infinite loop. PageSize:{0}")
    @ValueSource(ints = {1, 5, 10, 20, 99, 100, 101, 200})
    void shouldNotEnterAnInfiniteLoop(int pageSize) throws JsonProcessingException {
        insertRandomUsers(100);
        var firstEvent = sampleEventWithoutStartingPointer(pageSize);
        performEventDrivenScanInWholeDatabase(firstEvent);
    }

    @ParameterizedTest(name = "should apply migration action to all users in database.PageSize:{0}")
    @ValueSource(ints = {1, 5, 10, 20, 99, 100, 101, 200})
    void shouldApplyMigrationActionToAllUsersInDatabase(int pageSize) throws JsonProcessingException {
        var expectedFamilyName = randomString();
        final var insertedUsers = insertRandomUsers(100);
        var migrationService = new FamilyNameChangeMigration(expectedFamilyName);

        handler = new EventBasedScanHandler(localDynamo, eventClient, migrationService);
        final var expectedUsers = migrateUsersDirectly(insertedUsers, expectedFamilyName);

        var firstEvent = sampleEventWithoutStartingPointer(pageSize);
        performEventDrivenScanInWholeDatabase(firstEvent);
        var updatedUsers = scanAllUsersInDatabaseDirectly();
        assertThat(updatedUsers, contains(expectedUsers.toArray(UserDto[]::new)));
    }

    private List<UserDto> scanAllUsersInDatabaseDirectly() {
        return localDynamo.scanPaginator(ScanRequest.builder().tableName(USERS_AND_ROLES_TABLE).build())
            .stream()
            .flatMap(page -> page.items().stream())
            .filter(this::isUser)
            .map(UserDao.TABLE_SCHEMA::mapToItem)
            .map(UserDao::toUserDto)
            .collect(Collectors.toList());
    }

    private boolean isUser(Map<String, AttributeValue> entry) {
        return entry.get(Typed.TYPE_FIELD).s().equals(UserDao.TYPE_VALUE);
    }

    private void performEventDrivenScanInWholeDatabase(InputStream firstEvent) throws JsonProcessingException {
        handler.handleRequest(firstEvent, outputStream, CONTEXT);
        while (!eventClient.getRequestEntries().isEmpty()) {
            InputStream reconstructEvent = fetchNextEventFromEventBridgeClient();
            eventClient.getRequestEntries().remove(0);
            handler.handleRequest(reconstructEvent, outputStream, CONTEXT);
        }
    }

    private InputStream fetchNextEventFromEventBridgeClient() throws JsonProcessingException {
        PutEventsRequestEntry emittedEvent = eventClient.getRequestEntries().get(0);
        ScanDatabaseRequestV2 scanRequest = ScanDatabaseRequestV2.fromJson(emittedEvent.detail());
        return EventBridgeEventBuilder.sampleEvent(scanRequest);
    }

    private List<UserDto> migrateUsersDirectly(List<UserDto> insertedUsers, String expectedFilename) {
        return insertedUsers.stream()
            .map(user -> user.copy().withFamilyName(expectedFilename).build())
            .collect(Collectors.toList());
    }

    private UserDto fetchLastScannedUserUsingTheEmittedEvent(ScanDatabaseRequestV2 emittedScanRequest) {
        return attempt(emittedScanRequest::toDynamoScanMarker)
            .map(this::createGetItemRequest)
            .map(getItemRequest -> localDynamo.getItem(getItemRequest))
            .map(GetItemResponse::item)
            .map(UserDao.TABLE_SCHEMA::mapToItem)
            .map(UserDao::toUserDto)
            .orElseThrow();
    }

    private GetItemRequest createGetItemRequest(Map<String, AttributeValue> startMarker) {
        return GetItemRequest.builder()
            .tableName(USERS_AND_ROLES_TABLE)
            .key(startMarker)
            .build();
    }

    private UserDto calculateLastScannedUser(int pageSize, List<UserDto> insertedUsers) {
        return insertedUsers.get(indexOfLastDatabaseEntryInScannedPage(pageSize));
    }

    private int indexOfLastDatabaseEntryInScannedPage(int pageSize) {
        return pageSize - 1;
    }

    private ScanDatabaseRequestV2 extractScanRequestFromEmittedMessage() {
        return eventClient.getRequestEntries()
            .stream()
            .map(PutEventsRequestEntry::detail)
            .map(attempt(ScanDatabaseRequestV2::fromJson))
            .flatMap(Try::stream)
            .collect(SingletonCollector.collect());
    }

    private List<UserDto> insertRandomUsers(int numberOfUsers) {
        var unused = IntStream.range(0, numberOfUsers)
            .boxed()
            .map(attempt(i -> randomUser()))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
        return scanAllUsersInDatabaseDirectly();
    }

    private UserDto randomUser() {
        var user = UserDto.newBuilder().withUsername(randomString()).build();
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    private InputStream sampleEventWithoutStartingPointer(Integer pageSize) {
        return EventBridgeEventBuilder.sampleEvent(createSampleScanRequest(pageSize));
    }

    private ScanDatabaseRequestV2 createSampleScanRequest(Integer pageSize) {
        return new ScanDatabaseRequestV2(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC, pageSize, NO_START_MARKER);
    }

    private static class FamilyNameChangeMigration implements UserMigrationService {

        private final String familyName;
        private final ArrayList<UserDto> migratedUsers;

        private FamilyNameChangeMigration(String familyName) {
            this.familyName = familyName;
            migratedUsers = new ArrayList<>();
        }

        public ArrayList<UserDto> getMigratedUsers() {
            return migratedUsers;
        }

        @Override
        public UserDto migrateUser(UserDto userDto) {
            var migratedUser = userDto.copy().withFamilyName(familyName).build();
            migratedUsers.add(migratedUser);
            return migratedUser;
        }
    }
}