package no.unit.nva.customer;

import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.attempt.Failure;

import java.util.UUID;

import static nva.commons.core.attempt.Try.attempt;

public abstract class CustomerHandler<I> extends ApiGatewayHandler<I, CustomerDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";

    public CustomerHandler(Class<I> iclass) {
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

}
