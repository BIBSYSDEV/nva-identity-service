package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class IdentityServiceImplTest extends LocalIdentityService {

    public static final String EXPECTED_EXCEPTION_MESSAGE = "ExpectedExceptionMessage";

    private IdentityServiceImpl databaseService;

    @BeforeEach
    public void init() throws InvalidEntryInternalException {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());
    }

    @Test
    public void shouldThrowExceptionWhenClientThrowsException()
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
        DynamoDbClient failingClient = mockMapperThrowingException();
        return new IdentityServiceImpl(failingClient);
    }

    private DynamoDbClient mockMapperThrowingException() {
        DynamoDbClient failingClient = mock(DynamoDbClient.class);
        when(failingClient.getItem(any(GetItemRequest.class)))
            .thenAnswer(ignored -> {
                throw new RuntimeException(EXPECTED_EXCEPTION_MESSAGE);
            });
        return failingClient;
    }
}