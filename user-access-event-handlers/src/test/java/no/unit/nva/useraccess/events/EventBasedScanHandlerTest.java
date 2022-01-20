package no.unit.nva.useraccess.events;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccess.events.EventsConfig.IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.unit.nva.database.DatabaseAccessor;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBasedScanHandlerTest extends DatabaseAccessor {

    public static final Context CONTEXT = mock(Context.class);
    public static final Integer NO_PAGE_SIZE = null;
    public static final Map<String, AttributeValue> NO_START_MARKER = null;
    private ByteArrayOutputStream outputStream;
    private EventBasedScanHandler eventBasedScanHandler;

    private IdentityServiceImpl identityService;
    private EchoMigrationService migrationService;

    @BeforeEach
    public void init() {
        super.initializeTestDatabase();
        this.identityService = new IdentityServiceImpl(localDynamo);
        outputStream = new ByteArrayOutputStream();
        migrationService = new EchoMigrationService();
        eventBasedScanHandler = new EventBasedScanHandler(localDynamo, migrationService);
    }

    @Test
    void shouldScanSingleUserWhenSingleUserExistsInDatabaseAndInputRequestHasNoStartMarker()
        throws InvalidInputException, ConflictException, NotFoundException {
        var firstUser = randomUser();
        var inputEvent = sampleEventWithoutStartingPointer();
        eventBasedScanHandler.handleRequest(inputEvent, outputStream, CONTEXT);
        assertThat(migrationService.getScannedUsers(), contains(firstUser));
    }

    private UserDto randomUser() throws InvalidInputException, NotFoundException, ConflictException {
        var user = UserDto.newBuilder().withUsername(randomString()).build();
        identityService.addUser(user);
        return identityService.getUser(user);
    }

    private InputStream sampleEventWithoutStartingPointer() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(createSampleScanRequest());
    }

    private ScanDatabaseRequest createSampleScanRequest() {
        return new ScanDatabaseRequest(IDENTITY_SERVICE_BATCH_SCAN_EVENT_TOPIC, NO_PAGE_SIZE, NO_START_MARKER);
    }

    private static class EchoMigrationService implements MigrationService {

        private final List<UserDto> scannedUsers = new ArrayList<>();

        public List<UserDto> getScannedUsers() {
            return scannedUsers;
        }

        @Override
        public UserDto migrateUserDto(UserDto userDto) {
            scannedUsers.add(userDto);
            return userDto;
        }
    }
}