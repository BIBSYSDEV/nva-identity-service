package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class IdentityServiceImplTest extends DatabaseAccessor {

    public static final String EXPECTED_EXCEPTION_MESSAGE = "ExpectedExceptionMessage";

    private IdentityServiceImpl databaseService;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());
    }

    @Test
    public void getRoleExceptionWhenItReceivesInvalidRoleFromDatabase()
        throws InvalidEntryInternalException {

        IdentityService serviceThrowingException = mockServiceThrowsExceptionWhenLoadingRole();
        RoleDto sampleRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        Executable action = () -> serviceThrowingException.getRole(sampleRole);
        RuntimeException exception = assertThrows(RuntimeException.class, action);

        assertThat(exception.getMessage(), containsString(EXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void getRoleLogsWarningWhenNotFoundExceptionIsThrown() throws InvalidEntryInternalException {
        TestAppender testAppender = LogUtils.getTestingAppender(RoleService.class);
        RoleDto nonExistingRole = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        attempt(() -> databaseService.getRole(nonExistingRole));
        assertThat(testAppender.getMessages(),
                   StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    private IdentityService mockServiceThrowsExceptionWhenLoadingRole() {
        AmazonDynamoDB failingClient = mockMapperThrowingException();
        return new IdentityServiceImpl(failingClient);
    }

    private AmazonDynamoDB mockMapperThrowingException() {
        AmazonDynamoDB failingClient = mock(AmazonDynamoDB.class);
        when(failingClient.getItem(any(GetItemRequest.class)))
            .thenAnswer(ignored -> {
                throw new RuntimeException(EXPECTED_EXCEPTION_MESSAGE);
            });
        return failingClient;
    }
}