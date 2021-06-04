package no.unit.nva.database;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.Optional;
import no.unit.nva.useraccessmanagement.dao.RoleDb;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleService extends DatabaseSubService {

    public static final String ROLE_ALREADY_EXISTS_ERROR_MESSAGE = "Role already exists: ";

    public static final String ROLE_NOT_FOUND_MESSAGE = "Could not find role: ";
    public static final String GET_ROLE_DEBUG_MESSAGE = "Getting role:";

    public static final String ADD_ROLE_DEBUG_MESSAGE = "Adding role:";
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    protected RoleService(Table table) {
        super(table);
    }

    /**
     * Add role to the database.
     *
     * @param roleDto the role to be added.
     * @throws ConflictException             when a role with the same name exists.
     * @throws InvalidInputException         when the input entry is not valid.
     * @throws InvalidEntryInternalException when there is an entry in the database and that entry is not valid.
     */
    public void addRole(RoleDto roleDto) throws ConflictException, InvalidInputException,
                                                InvalidEntryInternalException {

        logger.debug(ADD_ROLE_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(roleDto));

        validate(roleDto);
        checkRoleDoesNotExist(roleDto);
        table.putItem(RoleDb.fromRoleDto(roleDto).toItem());
    }

    /**
     * Fetches a role from the database.
     *
     * @param queryObject the query object containing the rolename.
     * @return the Role that corresponds to the given rolename.
     * @throws NotFoundException             when a role with the specified name does not exist in the database.
     * @throws InvalidEntryInternalException when the role stored in the database has invalid stucture.
     */
    public RoleDto getRole(RoleDto queryObject) throws NotFoundException, InvalidEntryInternalException {
        return getRoleAsOptional(queryObject)
            .orElseThrow(() -> handleRoleNotFound(queryObject));
    }

    protected RoleDb fetchRoleDao(RoleDb queryObject) {
        Item item = fetchItem(queryObject);
        return (item != null) ? RoleDb.fromItem(item, RoleDb.class) : null;
    }

    private static NotFoundException handleRoleNotFound(RoleDto queryObject) {
        logger.debug(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName());
        return new NotFoundException(ROLE_NOT_FOUND_MESSAGE + queryObject.getRoleName());
    }

    private Optional<RoleDto> getRoleAsOptional(RoleDto queryObject) throws InvalidEntryInternalException {
        logger.debug(GET_ROLE_DEBUG_MESSAGE + convertToStringOrWriteErrorMessage(queryObject));
        return Optional.ofNullable(attemptFetchRole(queryObject));
    }

    private void checkRoleDoesNotExist(RoleDto roleDto) throws ConflictException, InvalidEntryInternalException {
        if (roleAlreadyExists(roleDto)) {
            throw new ConflictException(ROLE_ALREADY_EXISTS_ERROR_MESSAGE + roleDto.getRoleName());
        }
    }

    private boolean roleAlreadyExists(RoleDto roleDto) throws InvalidEntryInternalException {
        return getRoleAsOptional(roleDto).isPresent();
    }

    private RoleDto attemptFetchRole(RoleDto queryObject) throws InvalidEntryInternalException {
        RoleDb roledb = Try.of(queryObject)
            .map(RoleDb::fromRoleDto)
            .map(this::fetchRoleDao)
            .orElseThrow(DatabaseSubService::handleError);
        return nonNull(roledb) ? roledb.toRoleDto() : null;
    }
}
