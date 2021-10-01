package no.unit.nva.customer.testing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

public class CustomerDynamoDBLocal {

    public static final String ORG_NUMBER = "feideOrganizationId";
    public static final String CRISTIN_ID = "cristinId";
    public static final String NVA_CUSTOMERS_TABLE_NAME = "nva_customers";
    public static final String IDENTIFIER = "identifier";
    public static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    public static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";

    protected AmazonDynamoDB ddb;
    protected DynamoDB client;


    protected void setupDatabase() {
        ddb = DynamoDBEmbedded.create().amazonDynamoDB();
        createCustomerTable(ddb);
        client = new DynamoDB(ddb);
    }

    @AfterEach
    public void deleteDatabase() {
        if (ddb != null) {
            ddb.shutdown();
        }
    }

    public Table getTable() {
        return client.getTable(NVA_CUSTOMERS_TABLE_NAME);
    }

    public Index getIndex(String indexName) {
        return client.getTable(NVA_CUSTOMERS_TABLE_NAME).getIndex(indexName);
    }

    private void createCustomerTable(AmazonDynamoDB ddb) {
        List<AttributeDefinition> attributeDefinitions = asList(
            new AttributeDefinition(IDENTIFIER, ScalarAttributeType.S),
            new AttributeDefinition(ORG_NUMBER, ScalarAttributeType.S),
            new AttributeDefinition(CRISTIN_ID, ScalarAttributeType.S)
        );

        List<KeySchemaElement> keySchema = singletonList(
            new KeySchemaElement(IDENTIFIER, KeyType.HASH)
        );

        List<KeySchemaElement> byOrgNumberKeyScheme = singletonList(
            new KeySchemaElement(ORG_NUMBER, KeyType.HASH)
        );

        List<KeySchemaElement> byCristinIdKeyScheme = singletonList(
            new KeySchemaElement(CRISTIN_ID, KeyType.HASH)
        );

        Projection allProjection = new Projection()
            .withProjectionType(ProjectionType.ALL);

        List<GlobalSecondaryIndex> globalSecondaryIndexes = asList(
            new GlobalSecondaryIndex()
                .withIndexName(BY_ORG_NUMBER_INDEX_NAME)
                .withKeySchema(byOrgNumberKeyScheme)
                .withProjection(allProjection),
            new GlobalSecondaryIndex()
                .withIndexName(BY_CRISTIN_ID_INDEX_NAME)
                .withKeySchema(byCristinIdKeyScheme)
                .withProjection(allProjection)
        );

        CreateTableRequest createTableRequest =
            new CreateTableRequest()
                .withTableName(NVA_CUSTOMERS_TABLE_NAME)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(keySchema)
                .withGlobalSecondaryIndexes(globalSecondaryIndexes)
                .withBillingMode(BillingMode.PAY_PER_REQUEST);

        ddb.createTable(createTableRequest);
    }
}
