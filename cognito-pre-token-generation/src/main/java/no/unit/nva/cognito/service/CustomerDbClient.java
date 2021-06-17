package no.unit.nva.cognito.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.CustomerDb;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CustomerDbClient implements CustomerApi {

    public static final String ID_NAMESPACE_ENV = "ID_NAMESPACE";
    private static final Logger logger = LoggerFactory.getLogger(CustomerDbClient.class);
    public static final String CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER = "Customer not found for orgNumber={}";

    private CustomerService service;
    private CustomerMapper mapper;

    @JacocoGenerated
    public CustomerDbClient() {
        this(defaultCustomerService(),
                defaultCustomerMapper()
        );
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
                AmazonDynamoDBClientBuilder.defaultClient(),
                ObjectMapperConfig.objectMapper,
                new Environment());
    }

    @JacocoGenerated
    private static CustomerMapper defaultCustomerMapper() {
        String namespace = new Environment().readEnv(ID_NAMESPACE_ENV);
        return new CustomerMapper(namespace);
    }

    public CustomerDbClient(CustomerService service, CustomerMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public Optional<CustomerResponse> getCustomer(String orgNumber) {
        CustomerResponse response = null;
        try {
            CustomerDb customerDb = service.getCustomerByOrgNumber(orgNumber);
            response = toCustomerResponse(customerDb);
        } catch (ApiGatewayException e) {
            logger.error(CUSTOMER_NOT_FOUND_FOR_ORG_NUMBER, orgNumber, e);
        }
        return Optional.ofNullable(response);
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
