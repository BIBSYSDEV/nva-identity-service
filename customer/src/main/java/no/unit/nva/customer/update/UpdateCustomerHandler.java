package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class UpdateCustomerHandler extends CustomerHandler<CustomerDto> {

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
        super(CustomerDto.class);
        this.customerService = customerService;
    }

    @Override
    protected Integer getSuccessStatusCode(CustomerDto input, CustomerDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected CustomerDto processInput(CustomerDto input, RequestInfo requestInfo, Context context)
        throws InputException, NotFoundException, ForbiddenException {

        var currentCustomer = customerService.getCustomer(input.getId());
        if (isOnlyPublicationWorkflowUpdate(currentCustomer, input)) {
            authorizePublicationWorkflowUpdate(requestInfo);
        }

        UUID identifier = getIdentifier(requestInfo);
        return customerService.updateCustomer(identifier, input);
    }

    private void authorizePublicationWorkflowUpdate(RequestInfo requestInfo) throws ForbiddenException {
        if (doesNotHaveRequiredRights(requestInfo) || requestInfo.isApplicationAdmin()) {
            throw new ForbiddenException();
        }
    }

    private boolean doesNotHaveRequiredRights(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.ADMINISTRATE_APPLICATION.toString());
    }

    private boolean isOnlyPublicationWorkflowUpdate(CustomerDto currentCustomer, CustomerDto input) {
        var targetCustomer = currentCustomer
                                 .copy()
                                 .withPublicationWorkflow(input.getPublicationWorkflow())
                                 .build();
        return Objects.equals(input, targetCustomer);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
