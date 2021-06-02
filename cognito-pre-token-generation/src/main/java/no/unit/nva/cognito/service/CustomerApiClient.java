package no.unit.nva.cognito.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.function.Supplier;
import no.unit.nva.cognito.model.CustomerResponse;

import nva.commons.core.Environment;
import nva.commons.core.attempt.ConsumerWithException;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

public class CustomerApiClient implements CustomerApi {

    public static final String PATH = "/customer/orgNumber/";
    public static final String CUSTOMER_API_SCHEME = "CUSTOMER_API_SCHEME";
    public static final String CUSTOMER_API_HOST = "CUSTOMER_API_HOST";
    private static final Logger logger = LoggerFactory.getLogger(CustomerApiClient.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String customerApiScheme;
    private final String customerApiHost;

    public CustomerApiClient(HttpClient httpClient,
                             ObjectMapper objectMapper,
                             Environment environment) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.customerApiScheme = environment.readEnv(CUSTOMER_API_SCHEME);
        this.customerApiHost = environment.readEnv(CUSTOMER_API_HOST);
    }

    @Override
    public Optional<CustomerResponse> getCustomer(String orgNumber) {
        long start = currentTimeMillis();
        logger.info("Requesting customer information for orgNumber: " + orgNumber);
        try {
            var response = fetchCustomerInformation(orgNumber)
                .orElseThrow(getHttpClientInitializationError());
            if (response.statusCode() == HttpStatus.SC_NOT_FOUND) {
                logger.info("getCustomer success took {} ms", currentTimeMillis() - start);
                return Optional.empty();
            }
            if (response.statusCode() != HttpStatus.SC_OK) {
                logger.info("getCustomer failure took {} ms", currentTimeMillis() - start);
                logger.error("Error fetching customer information, API response was {}", response.statusCode());
                throw new IllegalStateException("Error fetching customer information");
            }
            logger.info("getCustomer success took {} ms", currentTimeMillis() - start);
            return Optional.ofNullable(this.parseCustomer(response));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing customer information", e);
            logger.info("getCustomer failure took {} ms", currentTimeMillis() - start);
            throw new IllegalStateException("Error parsing customer information");
        }
    }

    private Supplier<IllegalStateException> getHttpClientInitializationError() {
        return () ->
            new IllegalStateException("Error fetching customer information, http client failed to initialize.");
    }

    private Optional<HttpResponse<String>> fetchCustomerInformation(String orgNumber) {
        return Try.of(formUri(orgNumber))
            .map(URIBuilder::build)
            .map(this::buildHttpRequest)
            .map(this::sendHttpRequest)
            .toOptional(logResponseError());
    }

    private ConsumerWithException<Failure<HttpResponse<String>>, RuntimeException> logResponseError() {
        return failure -> logger.error("Error fetching customer information");
    }

    private CustomerResponse parseCustomer(HttpResponse<String> response)
        throws JsonProcessingException {
        return objectMapper.readValue(response.body(), CustomerResponse.class);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private URIBuilder formUri(String orgNumber) {
        return new URIBuilder()
            .setScheme(customerApiScheme)
            .setHost(customerApiHost)
            .setPath(PATH + orgNumber);
    }

    private HttpRequest buildHttpRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .build();
    }
}