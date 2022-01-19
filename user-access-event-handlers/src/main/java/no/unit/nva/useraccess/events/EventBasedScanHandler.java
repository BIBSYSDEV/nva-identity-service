package no.unit.nva.useraccess.events;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import java.util.List;
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
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBasedScanHandler
    extends DestinationsEventBridgeEventHandler<ScanDatabaseRequest, ScanDatabaseRequest> {

    private static final Logger logger = LoggerFactory.getLogger(EventBasedScanHandler.class);
    private final MigrationService migrationService;
    private final AmazonDynamoDB dynamoDbClient;
    private final String USERS_AND_ROLES_TABLE = new Environment().readEnv("USERS_AND_ROLES_TABLE");

    @JacocoGenerated
    public EventBasedScanHandler(AmazonDynamoDB dynamoDbClient, MigrationService migrationService) {
        super(ScanDatabaseRequest.class);
        this.migrationService = migrationService;
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    protected ScanDatabaseRequest processInputPayload(
        ScanDatabaseRequest input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<ScanDatabaseRequest>> event,
        Context context) {
        ScanRequest scanRequest = createScanRequest(input);

        List<UserDto> users = fetchOnePageOfUsers(scanRequest);
        var migratedUsers = users.stream().map(migrationService::migrateUserDto).collect(Collectors.toList());
        migratedUsers.forEach(user->logger.info("migratedUser:"+user.toJsonString()));
        return notImportant();
    }

    private List<UserDto> fetchOnePageOfUsers(ScanRequest scanRequest) {
        var result = dynamoDbClient.scan(scanRequest);
        return result.getItems().stream()
            .map(ItemUtils::toItem)
            .filter(this::databaseEntyIsUser)
            .map(Item::toJSON)
            .map(attempt(this::parseDao))
            .map(Try::orElseThrow)
            .map(UserDb::toUserDto)
            .collect(Collectors.toList());
    }

    private UserDb parseDao(String json) throws IOException {
        return JsonUtils.dtoObjectMapper.readValue(json, UserDb.class);
    }

    private boolean databaseEntyIsUser(Item item) {
        return item.getString(WithType.TYPE_FIELD).equals(UserDb.TYPE);
    }

    private ScanRequest createScanRequest(ScanDatabaseRequest input) {
        var scanRequest = new ScanRequest()
            .withTableName(USERS_AND_ROLES_TABLE)
            .withLimit(input.getPageSize())
            .withExclusiveStartKey(input.getStartMarker());
        return scanRequest;
    }

    private ScanDatabaseRequest notImportant() {
        return null;
    }
}
