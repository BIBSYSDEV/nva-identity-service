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
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JsonUtils;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class EventBasedScanHandlerTest extends DatabaseAccessor {

    public static final Context CONTEXT = mock(Context.class);
    public static final Integer NO_PAGE_SIZE = null;
    public static final Map<String, AttributeValue> NO_START_MARKER = null;
    public static final boolean SEQUENTIAL = false;
    private ByteArrayOutputStream outputStream;
    private EventBasedScanHandler handler;

    private IdentityServiceImpl identityService;
    private EchoMigrationService migrationService;
    private FakeEventBridgeClient eventClient;

    @BeforeEach
    public void init() {
        super.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(localDynamo);
        this.eventClient = new FakeEventBridgeClient();
        outputStream = new ByteArrayOutputStream();
        migrationService = new EchoMigrationService();
        handler = new EventBasedScanHandler(localDynamo, eventClient, migrationService);
    }

    @Test
    void shouldScanSingleUserWhenSingleUserExistsInDatabaseAndInputRequestHasNoStartMarker()
        throws InvalidInputException, ConflictException, NotFoundException {
        var firstUser = randomUser();
        var inputEvent = sampleEventWithoutStartingPointer(NO_PAGE_SIZE);
        handler.handleRequest(inputEvent, outputStream, CONTEXT);
        assertThat(migrationService.getScannedUsers(), contains(firstUser));
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
    void shouldScanAllUsersInDatabaseAndNotEnterAnInfiniteLoop(int pageSize) throws JsonProcessingException {
        final var insertedUsers = insertRandomUsers(100);
        var firstEvent = sampleEventWithoutStartingPointer(pageSize);
        performEventDrivenScanInWholeDatabase(firstEvent);
    }

    @ParameterizedTest(name = "should apply migration action to all users in database.PageSize:{0}")
    @ValueSource(ints = {1, 5, 10, 20, 99, 100, 101, 200})
    void shouldApplyMigrationActionToAllUsersInDatabase(int pageSize) throws JsonProcessingException {
        String expectedFamilyName = randomString();
        final var insertedUsers = insertRandomUsers(100);
        FamilyNameChangeMigration migrationService = new FamilyNameChangeMigration(expectedFamilyName);
        handler = new EventBasedScanHandler(localDynamo, eventClient, migrationService);
        final var expectedUsers = migrateUsersDirectly(insertedUsers, expectedFamilyName);
        var firstEvent = sampleEventWithoutStartingPointer(pageSize);
        performEventDrivenScanInWholeDatabase(firstEvent);
        var updatedUsers = scanAllUsersInDatabaseDirectly();
        assertThat(updatedUsers, contains(expectedUsers.toArray(UserDto[]::new)));
    }

    private List<UserDto> scanAllUsersInDatabaseDirectly() {
        Table table = new Table(localDynamo, USERS_AND_ROLES_TABLE);
        var items = table.scan().spliterator();
        return StreamSupport.stream(items, SEQUENTIAL)
            .filter(item -> UserDb.TYPE.equals(item.getString("type")))
            .map(Item::toJSON)
            .map(UserDb::fromJson)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());
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
        ScanDatabaseRequest scanRequest = ScanDatabaseRequest.fromJson(emittedEvent.detail());
        return EventBridgeEventBuilder.sampleEvent(scanRequest);
    }

    private List<UserDto> migrateUsersDirectly(List<UserDto> insertedUsers, String expectedFilename) {
        return insertedUsers.stream()
            .map(user -> user.copy().withFamilyName(expectedFilename).build())
            .collect(Collectors.toList());
    }

    private UserDto fetchLastScannedUserUsingTheEmittedEvent(ScanDatabaseRequest emittedScanRequest) {
        return attempt(emittedScanRequest::getStartMarker)
            .map(startMarker -> localDynamo.getItem(USERS_AND_ROLES_TABLE, startMarker))
            .map(GetItemResult::getItem)
            .map(ItemUtils::toItem)
            .map(Item::toJSON)
            .map(UserDb::fromJson)
            .map(UserDb::toUserDto)
            .orElseThrow();
    }

    private UserDto calculateLastScannedUser(int pageSize, List<UserDto> insertedUsers) {
        return insertedUsers.get(indexOfLastDatabaseEntryInScannedPage(pageSize));
    }

    private int indexOfLastDatabaseEntryInScannedPage(int pageSize) {
        return pageSize - 1;
    }

    private ScanDatabaseRequest extractScanRequestFromEmittedMessage() {
        return eventClient.getRequestEntries()
            .stream()
            .map(PutEventsRequestEntry::detail)
            .map(attempt(ScanDatabaseRequest::fromJson))
            .flatMap(Try::stream)
            .collect(SingletonCollector.collect());
    }

    private List<UserDto> insertRandomUsers(int numberOfUsers) {
        var unused = IntStream.range(0, numberOfUsers)
            .boxed()
            .map(attempt(i -> randomUser()))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
        return localDynamo.scan(new ScanRequest().withTableName(USERS_AND_ROLES_TABLE))
            .getItems()
            .stream()
            .map(ItemUtils::toItem)
            .map(Item::toJSON)
            .map(attempt(json -> JsonUtils.dtoObjectMapper.readValue(json, UserDb.class)))
            .map(Try::orElseThrow)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());
    }

    private UserDto randomUser() throws InvalidInputException, NotFoundException, ConflictException {
        var user = UserDto.newBuilder().withUsername(randomString()).build();
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    private InputStream sampleEventWithoutStartingPointer(Integer pageSize) {
        return EventBridgeEventBuilder.sampleEvent(createSampleScanRequest(pageSize));
    }

    private ScanDatabaseRequest createSampleScanRequest(Integer pageSize) {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC, pageSize, NO_START_MARKER);
    }

    private static class FamilyNameChangeMigration implements MigrationService {

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
        public UserDto migrateUserDto(UserDto userDto) {
            var migratedUser = userDto.copy().withFamilyName(familyName).build();
            migratedUsers.add(migratedUser);
            return migratedUser;
        }
    }
}