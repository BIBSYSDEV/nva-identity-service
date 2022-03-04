package no.unit.nva.customer;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import java.util.UUID;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.attempt.Failure;

public abstract class CustomerHandler<I> extends ApiGatewayHandlerV2<I, CustomerDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";

    protected UUID getIdentifier(APIGatewayProxyRequestEvent requestInfo) {
        String identifier = RequestUtils.getPathParameter(requestInfo, IDENTIFIER).orElse(null);
        return attempt(() -> UUID.fromString(identifier))
            .orElseThrow(fail -> handleIdentifierParsingError(identifier, fail));
    }

    private InputException handleIdentifierParsingError(String identifier, Failure<UUID> fail) {
        return new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, fail.getException());
    }

    protected CustomerDto parseInput(String input) {
        return attempt(() -> CustomerDto.fromJson(input))
            .orElseThrow(fail -> new BadRequestException("Could not parse input" + input, fail.getException()));
    }
}
