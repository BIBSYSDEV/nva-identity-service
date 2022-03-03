package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.database.IdentityService.USERS_AND_ROLES_TABLE;
import java.util.Optional;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class RoleService extends DatabaseSubService {

    public static final String ROLE_ALREADY_EXISTS_ERROR_MESSAGE = "Role already exists: ";

    public static final String ROLE_NOT_FOUND_MESSAGE = "Could not find role: ";
    public static final String GET_ROLE_DEBUG_MESSAGE = "Getting role:";

    public static final String ADD_ROLE_DEBUG_MESSAGE = "Adding role:{}";
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final DynamoDbTable<RoleDb> table;

    protected RoleService(DynamoDbClient client) {
        super(client);
        this.table = this.client.table(USERS_AND_ROLES_TABLE, RoleDb.TABLE_SCHEMA);
    }

    /**
     * Add role to the database.
     *
     * @param roleDto the role to be added.
     */
    public void addRole(RoleDto roleDto) {
        logger.debug(ADD_ROLE_DEBUG_MESSAGE, convertToStringOrWriteErrorMessage(roleDto));
        validate(roleDto);
        checkRoleDoesNotExist(roleDto);
        table.putItem(RoleDb.fromRoleDto(roleDto));
    }

    /**
     * Fetches a role from the database.
     *
     * @param queryObject the query object containing the rolename.
     * @return the Role that corresponds to the given rolename.
     */
    public RoleDto getRole(RoleDto queryObject) {
        return getRoleAsOptional(queryObject)
            .orElseThrow(() -> handleRoleNotFound(queryObject));
    }

    protected RoleDb fetchRoleDb(RoleDb queryObject) {
        return table.getItem(queryObject);
    }

    private static NotFoundException handleRoleNotFound(RoleDto queryObject) {
        logger.debug(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName());
        return new NotFoundException(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName());
    }

    private Optional<RoleDto> getRoleAsOptional(RoleDto queryObject) {
        logger.debug(GET_ROLE_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        return Optional.ofNullable(attemptFetchRole(queryObject));
    }

    private void checkRoleDoesNotExist(RoleDto roleDto) {
        if (roleAlreadyExists(roleDto)) {
            throw new ConflictException(ROLE_ALREADY_EXISTS_ERROR_MESSAGE + roleDto.getRoleName());
        }
    }

    private boolean roleAlreadyExists(RoleDto roleDto) {
        return getRoleAsOptional(roleDto).isPresent();
    }

    private RoleDto attemptFetchRole(RoleDto queryObject) {
        RoleDb roledb = Try.of(queryObject)
            .map(RoleDb::fromRoleDto)
            .map(this::fetchRoleDb)
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(roledb) ? roledb.toRoleDto() : null;
    }
}
