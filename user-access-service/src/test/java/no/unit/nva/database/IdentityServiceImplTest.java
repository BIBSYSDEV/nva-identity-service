package no.unit.nva.database;

import static no.unit.nva.database.RoleService.ROLE_NOT_FOUND_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Set;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
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

    private IdentityService databaseService;

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

    @Test
    void shouldSucceedUpdatingAnExistingRole() throws InvalidInputException, ConflictException, NotFoundException {
        var existingRole = EntityUtils.createRole(randomString(), MANAGE_DOI);
        databaseService.addRole(existingRole);

        var updatedAccessRights = Set.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS);
        var roleToUpdate = existingRole.copy().withAccessRights(updatedAccessRights).build();

        databaseService.updateRole(roleToUpdate);

        var updatedRole = databaseService.getRole(roleToUpdate);
        assertThat(updatedRole.getAccessRights(), containsInAnyOrder(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS));
    }

    @Test
    void updateRoleLogsWarningWhenNotFoundExceptionIsThrown() throws InvalidEntryInternalException {
        TestAppender testAppender = LogUtils.getTestingAppender(RoleService.class);
        RoleDto role = EntityUtils.createRole(EntityUtils.SOME_ROLENAME);
        assertThrows(NotFoundException.class, () -> databaseService.updateRole(role));
        assertThat(testAppender.getMessages(),
                   StringContains.containsString(ROLE_NOT_FOUND_MESSAGE));
    }

    @Test
    void shouldListAllUsersWhenDatabseAlsoIncludesRoles() throws ConflictException, InvalidInputException {
        databaseService.addUser(EntityUtils.createUser());
        databaseService.addUser(EntityUtils.createUser());
        databaseService.addRole(EntityUtils.createRole(randomString()));

        var users = databaseService.listAllUsers();
        assertThat(users, hasSize(2));
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