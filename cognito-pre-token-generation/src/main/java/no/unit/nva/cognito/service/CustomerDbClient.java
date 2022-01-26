package no.unit.nva.cognito.service;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerDbClient implements CustomerApi {

    public static final String CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER = "Customer not found for orgNumber=";

    private static final Logger logger = LoggerFactory.getLogger(CustomerDbClient.class);

    private final CustomerService service;

    public CustomerDbClient(CustomerService service) {
        this.service = service;
    }

    @Override
    public Optional<CustomerResponse> getCustomer(String orgNumber) {
        return attempt(() -> service.getCustomerByOrgNumber(orgNumber))
            .map(this::toCustomerResponse)
            .toOptional(fail -> handleError(fail, orgNumber));
    }

    private void handleError(Failure<CustomerResponse> fail, String orgNumber) {
        logger.error(CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER + orgNumber, fail.getException());
    }

    private CustomerResponse toCustomerResponse(CustomerDto customerDto) {
        return new CustomerResponse(
            customerDto.getId(),
            customerDto.getCristinId()
        );
    }
}
