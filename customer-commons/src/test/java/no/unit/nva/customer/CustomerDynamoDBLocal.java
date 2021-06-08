package no.unit.nva.customer;

import static com.amazonaws.services.dynamodbv2.model.BillingMode.PAY_PER_REQUEST;
import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.unit.nva.customer.model.CustomerDb.CRISTIN_ID;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import java.util.List;
import no.unit.nva.customer.model.CustomerDb;
import org.junit.rules.ExternalResource;

public class CustomerDynamoDBLocal extends ExternalResource {

    public static final String NVA_CUSTOMERS_TABLE_NAME = "nva_customers";
    public static final String IDENTIFIER = CustomerDb.IDENTIFIER;
    public  static final String BY_ORG_NUMBER_INDEX_NAME = "byOrgNumber";
    public  static final String BY_CRISTIN_ID_INDEX_NAME = "byCristinId";
    public static final String ORG_NUMBER = CustomerDb.ORG_NUMBER;

    private AmazonDynamoDB ddb;
    private DynamoDB client;

    @Override
    protected void before() throws Throwable {
        super.before();
        ddb = DynamoDBEmbedded.create().amazonDynamoDB();
        createCustomerTable(ddb);
        client = new DynamoDB(ddb);
    }

    public Table getTable() {
        return client.getTable(NVA_CUSTOMERS_TABLE_NAME);
    }

    public Index getIndex(String indexName) {
        return client.getTable(NVA_CUSTOMERS_TABLE_NAME).getIndex(indexName);
    }

    private void createCustomerTable(AmazonDynamoDB ddb) {
        List<AttributeDefinition> attributeDefinitions = asList(
                new AttributeDefinition(IDENTIFIER, S),
                new AttributeDefinition(ORG_NUMBER, S),
                new AttributeDefinition(CRISTIN_ID, S)
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
                .withBillingMode(PAY_PER_REQUEST);

        ddb.createTable(createTableRequest);
    }

    @Override
    protected void after() {
        super.after();
        if (ddb != null) {
            ddb.shutdown();
        }
    }
}
