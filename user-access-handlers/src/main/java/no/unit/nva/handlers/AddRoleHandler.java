package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.IdentityServiceImpl;
import no.unit.nva.useraccessservice.exceptions.DataSyncException;
import no.unit.nva.useraccessservice.model.RoleDto;
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
    protected RoleDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {

        var inputRole = RoleDto.fromJson(input);

        databaseService.addRole(inputRole);
        return getEventuallyConsistent(() -> getRole(inputRole))
            .orElseThrow(() -> new DataSyncException(ERROR_FETCHING_SAVED_ROLE + inputRole.getRoleName()));
    }

    private RoleDto getRole(RoleDto input) {
        return databaseService.getRole(input);
    }
}
