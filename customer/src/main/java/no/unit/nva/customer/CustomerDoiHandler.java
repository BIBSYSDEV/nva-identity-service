package no.unit.nva.customer;

import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.core.attempt.Try.attempt;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto.DoiAgentDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.attempt.Failure;

public abstract class CustomerDoiHandler<I> extends ApiGatewayHandler<I, DoiAgentDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";

    protected static final String CUSTOMER_DOI_AGENT_SECRETS_NAME = "dataCiteCustomerSecrets";


    public CustomerDoiHandler(Class<I> iclass) {
        super(iclass);
    }

    protected UUID getIdentifier(RequestInfo requestInfo) throws InputException {
        String identifier = RequestUtils.getPathParameter(requestInfo, IDENTIFIER).orElse(null);
        return attempt(() -> UUID.fromString(identifier))
                   .orElseThrow(fail -> handleIdentifierParsingError(identifier, fail));
    }

    private InputException handleIdentifierParsingError(String identifier, Failure<UUID> fail) {
        return new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, fail.getException());
    }

    protected void authorizeDoiAgentChange(RequestInfo requestInfo) throws ForbiddenException {
        if (notApplicationAdmin(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    protected void authorizeDoiAgentRead(RequestInfo requestInfo) throws ForbiddenException {
        if (notApplicationAdmin(requestInfo) && notInstAdmin(requestInfo) && notEditor(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean notEditor(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(EDIT_OWN_INSTITUTION_RESOURCES.toString());
    }

    private boolean notInstAdmin(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(EDIT_OWN_INSTITUTION_USERS.toString());

    }

    private boolean notApplicationAdmin(RequestInfo requestInfo) {
        return !requestInfo.userIsApplicationAdmin();
    }
}
