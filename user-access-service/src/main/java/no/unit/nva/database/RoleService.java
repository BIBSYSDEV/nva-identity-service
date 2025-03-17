package no.unit.nva.database;

import no.unit.nva.database.IdentityService.Constants;
import no.unit.nva.useraccessservice.dao.RoleDb;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;

import static java.util.Objects.nonNull;

public class RoleService extends DatabaseSubService {

    public static final String ROLE_ALREADY_EXISTS_ERROR_MESSAGE = "Role already exists: ";

    public static final String ROLE_NOT_FOUND_MESSAGE = "Could not find role: ";
    public static final String GET_ROLE_DEBUG_MESSAGE = "Getting role:";

    public static final String ADD_ROLE_DEBUG_MESSAGE = "Adding role:{}";
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final DynamoDbTable<RoleDb> table;

    protected RoleService(DynamoDbClient client) {
        super(client);
        this.table = this.client.table(Constants.USERS_AND_ROLES_TABLE, RoleDb.TABLE_SCHEMA);
    }

    /**
     * Add role to the database.
     *
     * @param roleDto the role to be added.
     */
    public void addRole(RoleDto roleDto) throws ConflictException, InvalidInputException {
        logger.debug(ADD_ROLE_DEBUG_MESSAGE, convertToStringOrWriteErrorMessage(roleDto));
        validate(roleDto);
        checkRoleDoesNotExist(roleDto);
        table.putItem(RoleDb.fromRoleDto(roleDto));
    }

    private void checkRoleDoesNotExist(RoleDto roleDto) throws ConflictException {
        if (roleAlreadyExists(roleDto)) {
            throw new ConflictException(ROLE_ALREADY_EXISTS_ERROR_MESSAGE + roleDto.getRoleName());
        }
    }

    private boolean roleAlreadyExists(RoleDto roleDto) {
        return getRoleAsOptional(roleDto).isPresent();
    }

    private Optional<RoleDto> getRoleAsOptional(RoleDto queryObject) {
        logger.debug(GET_ROLE_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        return Optional.ofNullable(attemptFetchRole(queryObject));
    }

    private RoleDto attemptFetchRole(RoleDto queryObject) {
        RoleDb roledb = Try.of(queryObject)
            .map(RoleDb::fromRoleDto)
            .map(this::fetchRoleDb)
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(roledb) ? roledb.toRoleDto() : null;
    }

    protected RoleDb fetchRoleDb(RoleDb queryObject) {
        return table.getItem(queryObject);
    }

    /**
     * Fetches a role from the database.
     *
     * @param queryObject the query object containing the rolename.
     * @return the Role that corresponds to the given rolename.
     */
    public RoleDto getRole(RoleDto queryObject) throws NotFoundException {
        return getRoleAsOptional(queryObject)
            .orElseThrow(() -> handleRoleNotFound(queryObject));
    }

    private static NotFoundException handleRoleNotFound(RoleDto queryObject) {
        logger.debug(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName());
        return new NotFoundException(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName().getValue());
    }

    public void updateRole(RoleDto roleToUpdate) throws NotFoundException, InvalidInputException {
        validate(roleToUpdate);

        var originalRole = getRoleAsOptional(roleToUpdate)
            .orElseThrow(() -> handleRoleNotFound(roleToUpdate));

        var updatedRole = originalRole.copy().withAccessRights(roleToUpdate.getAccessRights()).build();
        table.putItem(RoleDb.fromRoleDto(updatedRole));
    }
}
