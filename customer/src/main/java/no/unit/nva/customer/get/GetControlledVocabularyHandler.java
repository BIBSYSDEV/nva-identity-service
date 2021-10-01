package no.unit.nva.customer.get;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.customer.Constants;
import no.unit.nva.customer.ObjectMapperConfig;
import no.unit.nva.customer.model.VocabularySettingDto;
import no.unit.nva.customer.model.interfaces.VocabularySettingsList;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaTypes;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetControlledVocabularyHandler extends ApiGatewayHandler<Void, VocabularySettingsList> {

    public static final String IDENTIFIER_PATH_PARAMETER = "identifier";
    public static final Environment ENVIRONMENT = new Environment();
    private static final List<MediaType> SUPPORTED_MEDIA_TYPES =
        List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
    private static final String AWS_REGION = Constants.AWS_REGION;
    private final CustomerService customerService;

    @JacocoGenerated
    public GetControlledVocabularyHandler() {
        this(defaultCustomerService());
    }

    public GetControlledVocabularyHandler(CustomerService customerService) {
        super(Void.class);
        this.customerService = customerService;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return SUPPORTED_MEDIA_TYPES;
    }

    @Override
    protected VocabularySettingsList processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        UUID identifier = UUID.fromString(requestInfo.getPathParameter(IDENTIFIER_PATH_PARAMETER));
        Set<VocabularySettingDto> vocabularySettings = customerService.getCustomer(identifier).
            getVocabularySettings();
        return new VocabularySettingsList(vocabularySettings);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, VocabularySettingsList output) {
        return HttpURLConnection.HTTP_OK;
    }

    private static CustomerService defaultCustomerService() {
        AmazonDynamoDB client = defaultDynamoDbClient();

        return new DynamoDBCustomerService(client, ObjectMapperConfig.objectMapper, ENVIRONMENT);
    }

    private static AmazonDynamoDB defaultDynamoDbClient() {
        EndpointConfiguration endpointConfiguration =
            new EndpointConfiguration(Constants.AWD_DYNAMODB_SERVICE_END_POINT, Constants.AWS_REGION);
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new DefaultAWSCredentialsProviderChain())
            .withRegion(AWS_REGION)
            .withEndpointConfiguration(endpointConfiguration)
            .build();
    }
}
