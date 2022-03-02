package no.unit.nva.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessmanagement.exceptions.BadRequestException;
import no.unit.nva.useraccessmanagement.exceptions.DataSyncException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import nva.commons.apigatewayv2.exceptions.ConflictException;
import nva.commons.apigatewayv2.exceptions.NotFoundException;
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
        super();
        this.databaseService = databaseService;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, RoleDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected RoleDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context)
        throws DataSyncException, ConflictException, InvalidInputException, BadRequestException {

        var inputRole = attempt(() -> JSON.std.beanFrom(RoleDto.class, input))
            .orElseThrow(fail -> new BadRequestException(fail.getException().getMessage()));
        databaseService.addRole(inputRole);
        return getEventuallyConsistent(() -> getRole(inputRole))
            .orElseThrow(() -> new DataSyncException(ERROR_FETCHING_SAVED_ROLE + inputRole.getRoleName()));
    }

    private RoleDto getRole(RoleDto input) throws NotFoundException {
        return databaseService.getRole(input);
    }
}
