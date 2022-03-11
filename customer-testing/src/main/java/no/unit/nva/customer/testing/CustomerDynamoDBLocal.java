package no.unit.nva.customer.testing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.unit.nva.customer.service.impl.DynamoDBCustomerService.CUSTOMERS_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class CustomerDynamoDBLocal {

    public static final String ORG_NUMBER = "feideOrganizationId";
    public static final String CRISTIN_ID = "cristinId";
    public static final String NVA_CUSTOMERS_TABLE_NAME = CUSTOMERS_TABLE_NAME;
    public static final String IDENTIFIER = "identifier";
    public static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";
    public static final long NOT_IMPORTANT = 100L;
    protected DynamoDbClient dynamoClient;

    public DynamoDbClient getDynamoClient() {
        return dynamoClient;
    }

    @AfterEach
    @SuppressWarnings("PMD.NullAssignment")
    public void deleteDatabase() {
        if (dynamoClient != null) {
            dynamoClient = null;
        }
    }

    public void setupDatabase() {
        dynamoClient = DynamoDBEmbedded.create().dynamoDbClient();
        createCustomerTable(dynamoClient);
    }

    private void createCustomerTable(DynamoDbClient ddb) {
        List<AttributeDefinition> attributeDefinitions = asList(
            AttributeDefinition.builder().attributeName(IDENTIFIER).attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName(ORG_NUMBER).attributeType(ScalarAttributeType.S).build(),
            AttributeDefinition.builder().attributeName(CRISTIN_ID).attributeType(ScalarAttributeType.S).build()
        );

        List<KeySchemaElement> keySchema = singletonList(
            KeySchemaElement.builder().attributeName(IDENTIFIER).keyType(KeyType.HASH).build()
        );

        List<KeySchemaElement> byOrgNumberKeyScheme = singletonList(
            KeySchemaElement.builder().attributeName(ORG_NUMBER).keyType(KeyType.HASH).build()
        );

        List<KeySchemaElement> byCristinIdKeyScheme = singletonList(
            KeySchemaElement.builder().attributeName(CRISTIN_ID).keyType(KeyType.HASH).build()
        );

        Projection allProjection = Projection.builder()
            .projectionType(ProjectionType.ALL)
            .build();

        List<GlobalSecondaryIndex> globalSecondaryIndexes = asList(
            createGsi(byOrgNumberKeyScheme, allProjection, BY_ORG_NUMBER_INDEX_NAME),
            createGsi(byCristinIdKeyScheme, allProjection, BY_CRISTIN_ID_INDEX_NAME)
        );

        CreateTableRequest createTableRequest =
            CreateTableRequest.builder()
                .tableName(NVA_CUSTOMERS_TABLE_NAME)
                .attributeDefinitions(attributeDefinitions)
                .keySchema(keySchema)
                .globalSecondaryIndexes(globalSecondaryIndexes)
                .provisionedThroughput(provisionedThroughput())
                .build();

        ddb.createTable(createTableRequest);
    }

    private GlobalSecondaryIndex createGsi(List<KeySchemaElement> byCristinIdKeyScheme, Projection allProjection,
                                           String cristinId) {
        return GlobalSecondaryIndex.builder()
            .indexName(cristinId)
            .keySchema(byCristinIdKeyScheme)
            .projection(allProjection)
            .provisionedThroughput(provisionedThroughput())
            .build();
    }

    private ProvisionedThroughput provisionedThroughput() {
        return ProvisionedThroughput.builder()
            .readCapacityUnits(NOT_IMPORTANT)
            .writeCapacityUnits(NOT_IMPORTANT)
            .build();
    }
}
