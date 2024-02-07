package no.unit.nva.customer.update;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.apigateway.RequestInfoConstants.SCOPES_CLAIM;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.CustomerHandler;
import no.unit.nva.customer.exception.InputException;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class UpdateCustomerHandler extends CustomerHandler<CustomerDto> {

    public static final String IDENTIFIER = "identifier";
    public static final String IDENTIFIER_IS_NOT_A_VALID_UUID = "Identifier is not a valid UUID: ";
    public static final String AWS_COGNITO_SIGNIN_USER_ADMIN = "aws.cognito.signin.user.admin";
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

        if (!isAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
        UUID identifier = getIdentifier(requestInfo);
        return customerService.updateCustomer(identifier, input);
    }

    private boolean isAuthorized(RequestInfo requestInfo) {
        return canManageOwnAffiliations(requestInfo)
               || canManageAllCustomers(requestInfo)
               || requestInfo.clientIsInternalBackend()
               || isCognitoAdmin(requestInfo);
    }

    private boolean canManageAllCustomers(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(MANAGE_CUSTOMERS);
    }
    private boolean canManageOwnAffiliations(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION);
    }

    private boolean isCognitoAdmin(RequestInfo requestInfo) {
        return requestInfo.getRequestContextParameterOpt(SCOPES_CLAIM).map(
            value -> value.contains(AWS_COGNITO_SIGNIN_USER_ADMIN)).orElse(false);
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return Constants.DEFAULT_RESPONSE_MEDIA_TYPES;
    }
}
