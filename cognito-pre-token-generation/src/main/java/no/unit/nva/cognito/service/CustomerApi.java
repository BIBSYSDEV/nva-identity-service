package no.unit.nva.cognito.service;

import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;

public interface CustomerApi {

    Optional<CustomerResponse> getCustomer(String orgNumber);
}
