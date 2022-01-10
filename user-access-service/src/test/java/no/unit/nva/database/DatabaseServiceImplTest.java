package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseServiceImplTest extends DatabaseAccessor {

    public static final String SOME_INSTITUTION = "someInstitution";
    public static final String SOME_USERNAME = "someUsername";
    public static final String EXPECTED_EXCEPTION_MESSAGE = "ExpectedExceptionMessage";

    private UserDto someUser;
    private DatabaseServiceImpl databaseService;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {

        someUser = UserDto.newBuilder().withUsername(SOME_USERNAME).build();
        databaseService = new DatabaseServiceImpl(initializeTestDatabase());
    }

//    @Test
//    public void getRoleExceptionWhenItReceivesInvalidRoleFromDatabase()
//        throws InvalidEntryInternalException {
//
//        DatabaseService serviceThrowingException = mockServiceThrowsExceptionWhenLoadingRole();
//        RoleDto sampleRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
//        Executable action = () -> serviceThrowingException.getRole(sampleRole);
//        RuntimeException exception = assertThrows(RuntimeException.class, action);
//
//        assertThat(exception.getMessage(), containsString(EXPECTED_EXCEPTION_MESSAGE));
//    }
//
//    @Test
//    public void getRoleThrowsInvalidEntryInternalExceptionWhenItReceivesInvalidRoleFromDatabase()
//        throws InvalidEntryInternalException {
//
//        DatabaseService service = mockServiceReceivingInvalidRoleDbInstance();
//        RoleDto sampleRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
//        Executable action = () -> service.getRole(sampleRole);
//        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
//
//        String expectedMessageContent = RoleDto.MISSING_ROLE_NAME_ERROR;
//        assertThat(exception.getMessage(), containsString(expectedMessageContent));
//    }

    @Test
    public void getRoleLogsWarningWhenNotFoundExceptionIsThrown() throws InvalidEntryInternalException {
        TestAppender testAppender = LogUtils.getTestingAppender(RoleService.class);
        RoleDto nonExistingRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        attempt(() -> databaseService.getRole(nonExistingRole));
        assertThat(testAppender.getMessages(),
            StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }


//    private DatabaseService mockServiceReceivingInvalidUserDbInstance() {
//        UserDb userWithoutUsername = new UserDb();
//        Table table = mockTableReturningInvalidEntry(userWithoutUsername);
//        return new DatabaseServiceImpl(table);
//    }

//    private DatabaseService mockServiceReceivingInvalidRoleDbInstance() {
//        RoleDb roleWithoutName = new RoleDb();
//
//        Table table = mockTableReturningInvalidEntry(roleWithoutName);
//        return new DatabaseServiceImpl(table);
//    }
//
//    private DatabaseService mockServiceThrowsExceptionWhenLoadingRole() {
//        Table mockMapper = mockMapperThrowingException();
//        return new DatabaseServiceImpl(mockMapper);
//    }

//    private Table mockMapperThrowingException() {
//        Table table = mock(Table.class);
//        when(table.getItem(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
//            throw new AmazonDynamoDBException(EXPECTED_EXCEPTION_MESSAGE);
//        });
//        return table;
//    }


}