package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import no.unit.nva.useraccessmanagement.dao.DynamoEntryWithRangeKey;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class DatabaseServiceImplTest extends DatabaseAccessor {

    public static final String SOME_INSTITUTION = "someInstitution";
    public static final String SOME_USERNAME = "someUsername";
    public static final String EXPECTED_EXCEPTION_MESSAGE = "ExpectedExceptionMessage";

    private UserDto someUser;
    private DatabaseServiceImpl databaseService;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {

        someUser = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        databaseService = new DatabaseServiceImpl(initializeTestDatabase(), envWithTableName);
    }

    @Test
    public void getUserThrowsInvalidEntryInternalExceptionWhenItReceivesInvalidUserFromDatabase() {

        UserDb userWithoutUsername = new UserDb();
        userWithoutUsername.setInstitution(SOME_INSTITUTION);

        DatabaseService service = mockServiceReceivingInvalidUserDbInstance();

        Executable action = () -> service.getUser(someUser);
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);

        String expectedMessageContent = UserDto.MISSING_FIELD_ERROR;
        assertThat(exception.getMessage(), containsString(expectedMessageContent));
    }

    @Test
    public void getRoleExceptionWhenItReceivesInvalidRoleFromDatabase()
        throws InvalidEntryInternalException {

        DatabaseService serviceThrowingException = mockServiceThrowsExceptionWhenLoadingRole();
        RoleDto sampleRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        Executable action = () -> serviceThrowingException.getRole(sampleRole);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        assertThat(exception.getMessage(), containsString(EXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void getRoleThrowsInvalidEntryInternalExceptionWhenItReceivesInvalidRoleFromDatabase()
        throws InvalidEntryInternalException {

        DatabaseService service = mockServiceReceivingInvalidRoleDbInstance();
        RoleDto sampleRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        Executable action = () -> service.getRole(sampleRole);
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);

        String expectedMessageContent = RoleDto.MISSING_ROLE_NAME_ERROR;
        assertThat(exception.getMessage(), containsString(expectedMessageContent));
    }

    @Test
    public void getRoleLogsWarningWhenNotFoundExceptionIsThrown() throws InvalidEntryInternalException {
        TestAppender testAppender = LogUtils.getTestingAppender(RoleService.class);
        RoleDto nonExistingRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        attempt(() -> databaseService.getRole(nonExistingRole));
        assertThat(testAppender.getMessages(),
            StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }


    private DatabaseService mockServiceReceivingInvalidUserDbInstance() {
        UserDb userWithoutUsername = new UserDb();
        Table table = mockTableReturningInvalidEntry(userWithoutUsername);
        return new DatabaseServiceImpl(table);
    }

    private DatabaseService mockServiceReceivingInvalidRoleDbInstance() {
        RoleDb roleWithoutName = new RoleDb();

        Table table = mockTableReturningInvalidEntry(roleWithoutName);
        return new DatabaseServiceImpl(table);
    }

    private DatabaseService mockServiceThrowsExceptionWhenLoadingRole() {
        Table mockMapper = mockMapperThrowingException();
        return new DatabaseServiceImpl(mockMapper);
    }

    private Table mockMapperThrowingException() {
        Table table = mock(Table.class);
        when(table.getItem(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            throw new AmazonDynamoDBException(EXPECTED_EXCEPTION_MESSAGE);
        });
        return table;
    }

    private Table mockTableReturningInvalidEntry(DynamoEntryWithRangeKey response) {
        Table mockMapper = mock(Table.class);
        when(mockMapper.getItem(anyString(), anyString(), anyString(), anyString())).thenReturn(response.toItem());
        return mockMapper;
    }
}