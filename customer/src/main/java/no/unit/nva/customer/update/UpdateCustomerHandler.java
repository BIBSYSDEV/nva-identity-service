package no.unit.nva.customer.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static no.unit.nva.customer.Constants.defaultCustomerService;

public class UpdateCustomerHandler extends ApiGatewayHandler<CustomerDto, CustomerDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    private final CustomerService customerService;

    /**
     * Default Constructor for UpdateCustomerHandler.
     */
    @JacocoGenerated
    public UpdateCustomerHandler() {
        this(defaultCustomerService(), new Environment());
    }

    /**
     * Constructor for UpdateCustomerHandler.
     *
     * @param customerService customerService
     * @param environment     environment
     */
    public UpdateCustomerHandler(
        CustomerService customerService,
        Environment environment) {
        super(CustomerDto.class, environment);
        this.customerService = customerService;
    }

    protected UUID getIdentifier(RequestInfo requestInfo) throws ApiGatewayException {
        String identifier = null;
        try {
            identifier = requestInfo.getPathParameters().get(IDENTIFIER);
            return UUID.fromString(identifier);
        } catch (Exception e) {
            throw new InputException(IDENTIFIER_IS_NOT_A_VALID_UUID + identifier, e);
        }
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected CustomerDto processInput(CustomerDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = getIdentifier(requestInfo);
        return customerService.updateCustomer(identifier, input);
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerDto input, CustomerDto output) {
        return HttpStatus.SC_OK;
    }
}
