package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.exceptions.DataSyncException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class AddRoleHandler extends HandlerWithEventualConsistency<RoleDto, RoleDto> {

    public static final String ERROR_FETCHING_SAVED_ROLE = "Could not fetch role with name: ";
    private final IdentityService databaseService;

    /**
     * Default constructor.
     */
    @JacocoGenerated
    public AddRoleHandler() {
        this(new IdentityServiceImpl());
    }

    public AddRoleHandler(IdentityService databaseService) {
        super(RoleDto.class);
        this.databaseService = databaseService;
    }

    @Override
    protected Integer getSuccessStatusCode(RoleDto input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected RoleDto processInput(RoleDto input, RequestInfo requestInfo, Context context)
        throws InvalidInputException, ConflictException, DataSyncException {

        databaseService.addRole(input);
        return getEventuallyConsistent(() -> getRole(input))
            .orElseThrow(() -> new DataSyncException(ERROR_FETCHING_SAVED_ROLE + input.getRoleName()));
    }

    private RoleDto getRole(RoleDto input) throws NotFoundException {
        return databaseService.getRole(input);
    }
}
