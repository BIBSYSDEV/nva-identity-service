package no.unit.nva.useraccess.events;

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
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(createSampleScanRequest(pageSize));
    }

    private ScanDatabaseRequest createSampleScanRequest(Integer pageSize) {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC, pageSize, NO_START_MARKER);
    }


}