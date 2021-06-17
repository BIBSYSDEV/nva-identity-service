package no.unit.nva.cognito.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static no.unit.nva.cognito.Constants.AWS_REGION_VALUE;
import static no.unit.nva.cognito.Constants.ENVIRONMENT;
import static no.unit.nva.cognito.Constants.ID_NAMESPACE_VALUE;
import static nva.commons.core.attempt.Try.attempt;

public class CustomerDbClient implements CustomerApi {

    public static final String CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER = "Customer not found for orgNumber=";

    private static final Logger logger = LoggerFactory.getLogger(CustomerDbClient.class);

    private final CustomerService service;
    private final CustomerMapper mapper;

    @JacocoGenerated
    public CustomerDbClient() {
        this(defaultCustomerService(),
                defaultCustomerMapper()
        );
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
                AmazonDynamoDBClientBuilder
                        .standard()
                        .withRegion(AWS_REGION_VALUE)
                        .build(),
                ObjectMapperConfig.objectMapper,
                ENVIRONMENT);
    }

    @JacocoGenerated
    private static CustomerMapper defaultCustomerMapper() {
        return new CustomerMapper(ID_NAMESPACE_VALUE);
    }

    public CustomerDbClient(CustomerService service, CustomerMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public Optional<CustomerResponse> getCustomer(String orgNumber) {
        return attempt(() -> service.getCustomerByOrgNumber(orgNumber))
                .map(this::toCustomerResponse)
                .toOptional(fail -> handleError(fail, orgNumber));
    }

    private void handleError(Failure<CustomerResponse> fail, String orgNumber) {
        logger.error(CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER+orgNumber, fail.getException());
    }

    private CustomerResponse toCustomerResponse(CustomerDb customerDb) {
        // Mapping to CustomerDto because we want the id with namespace
        CustomerDto customerDto = mapper.toCustomerDto(customerDb);
        return new CustomerResponse(
                customerDto.getId().toString(),
                customerDto.getCristinId()
        );
    }
}
