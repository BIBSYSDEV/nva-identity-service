package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class AddRoleHandler extends HandlerWithEventualConsistency<RoleDto, RoleDto> {

    public static final String ERROR_FETCHING_SAVED_ROLE = "Could not fetch role with name: ";
    private final IdentityService databaseService;

    /**
     * Default constructor.
     */
    @JacocoGenerated
    public AddRoleHandler() {
        this(new Environment(), new IdentityServiceImpl()
        );
    }

    public AddRoleHandler(
        Environment environment,
        IdentityService databaseService
    ) {
        super(RoleDto.class, environment);
        this.databaseService = databaseService;
    }

    @Override
    protected RoleDto processInput(RoleDto input, RequestInfo requestInfo, Context context)
        throws DataSyncException, ConflictException, InvalidInputException {
        databaseService.addRole(input);
        return getEventuallyConsistent(() -> getRole(input))
            .orElseThrow(() -> new DataSyncException(ERROR_FETCHING_SAVED_ROLE + input.getRoleName()));
    }

    private RoleDto getRole(RoleDto input) throws NotFoundException {
        return databaseService.getRole(input);
    }

    @Override
    protected Integer getSuccessStatusCode(RoleDto input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
