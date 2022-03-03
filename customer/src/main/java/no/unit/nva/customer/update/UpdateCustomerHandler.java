package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.RestConfig;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class UpdateCustomerHandler extends ApiGatewayHandlerV2<CustomerDto, CustomerDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    private final CustomerService customerService;

    /**
     * Default Constructor for UpdateCustomerHandler.
     */
    @JacocoGenerated
    public UpdateCustomerHandler() {
        this(defaultCustomerService());
    }

    /**
     * Constructor for UpdateCustomerHandler.
     *
     * @param customerService customerService
     */
    public UpdateCustomerHandler(CustomerService customerService) {
        super();
        this.customerService = customerService;
    }

    protected UUID getIdentifier(APIGatewayProxyRequestEvent requestInfo) {
        String identifier = null;
        try {
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(String input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
        UUID identifier = getIdentifier(requestInfo);
        var parsedCustomer = parseInput(input);
        return customerService.updateCustomer(identifier, parsedCustomer);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    private CustomerDto parseInput(String input) {
        return attempt(() -> RestConfig.defaultRestObjectMapper.beanFrom(CustomerDto.class, input))
            .orElseThrow(fail -> new BadRequestException("Could not parse input" + input, fail.getException()));
    }
}
