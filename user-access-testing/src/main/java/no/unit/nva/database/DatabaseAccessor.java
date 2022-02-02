package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.database.IdentityService.USERS_AND_ROLES_TABLE;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_RANGE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.database.interfaces.WithEnvironment;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

public abstract class DatabaseAccessor implements WithEnvironment {


    public static final int SINGLE_TABLE_EXPECTED = 1;
    public static final String STRING = "S";
    private static final Long CAPACITY_DOES_NOT_MATTER = 1000L;


    protected DynamoDbClient localDynamo;
    protected IdentityService databaseService;

    public IdentityServiceImpl createDatabaseServiceUsingLocalStorage() {
        databaseService = new IdentityServiceImpl(initializeTestDatabase());

        //return the field just to not break the current API.
        //TODO: remove return after merging.
        return (IdentityServiceImpl) databaseService;
    }

    /**
     * Initializes a local database. The client is stored in the {@code localDynamo variable}
     *
     * @return a client connected to the local database
     */
    public DynamoDbClient initializeTestDatabase() {

        localDynamo = createLocalDynamoDbMock();

        String tableName = USERS_AND_ROLES_TABLE;
        CreateTableResponse createTableResult = createTable(localDynamo, tableName);
        TableDescription tableDescription = createTableResult.tableDescription();
        assertEquals(tableName, tableDescription.tableName());
        assertThatTableKeySchemaContainsBothKeys(tableDescription.keySchema());
        assertEquals(TableStatus.ACTIVE, tableDescription.tableStatus());
        assertThat(tableDescription.tableArn(), containsString(tableName));

        ListTablesResponse tables = localDynamo.listTables();
        assertEquals(SINGLE_TABLE_EXPECTED, tables.tableNames().size());
        return localDynamo;
    }

    /**
     * Closes db.
     */
    @AfterEach
    @SuppressWarnings("PMD.NullAssignment")
    public void closeDB() {
        if (nonNull(localDynamo)) {
            localDynamo = null;
        }
    }

    private static CreateTableResponse createTable(DynamoDbClient client, String tableName) {
        List<AttributeDefinition> attributeDefinitions = defineKeyAttributes();
        List<KeySchemaElement> keySchema = defineKeySchema();
        ProvisionedThroughput provisionedthroughput = provisionedThroughputForLocalDatabase();

        CreateTableRequest request =
            CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(attributeDefinitions)
                .keySchema(keySchema)
                .provisionedThroughput(provisionedthroughput)
                .globalSecondaryIndexes(searchByInstitutionSecondaryIndex())
                .build();

        return client.createTable(request);
    }

    private static GlobalSecondaryIndex searchByInstitutionSecondaryIndex() {
        ProvisionedThroughput provisionedthroughput = provisionedThroughputForLocalDatabase();

        return GlobalSecondaryIndex
            .builder()
            .indexName(SEARCH_USERS_BY_INSTITUTION_INDEX_NAME)
            .keySchema(
                KeySchemaElement.builder().attributeName(SECONDARY_INDEX_1_HASH_KEY).keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName(SECONDARY_INDEX_1_RANGE_KEY).keyType(KeyType.RANGE).build()
            )
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .provisionedThroughput(provisionedthroughput)
            .build();
    }

    private static List<KeySchemaElement> defineKeySchema() {
        List<KeySchemaElement> keySchemaElements = new ArrayList<>();
        keySchemaElements
            .add(KeySchemaElement.builder().attributeName(PRIMARY_KEY_HASH_KEY).keyType(KeyType.HASH).build());
        keySchemaElements.add(
            KeySchemaElement.builder().attributeName(PRIMARY_KEY_RANGE_KEY).keyType(KeyType.RANGE).build());
        return keySchemaElements;
    }

    private static List<AttributeDefinition> defineKeyAttributes() {
        List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(AttributeDefinition.builder()
                                     .attributeName(PRIMARY_KEY_HASH_KEY)
                                     .attributeType(STRING).build()
        );
        attributeDefinitions.add(AttributeDefinition.builder()
                                     .attributeName(PRIMARY_KEY_RANGE_KEY)
                                     .attributeType(ScalarAttributeType.S)
                                     .build());

        attributeDefinitions.add(AttributeDefinition.builder()
                                     .attributeName(SECONDARY_INDEX_1_HASH_KEY)
                                     .attributeType(ScalarAttributeType.S)
                                     .build()
        );
        attributeDefinitions.add(AttributeDefinition.builder()
                                     .attributeName(SECONDARY_INDEX_1_RANGE_KEY)
                                     .attributeType(ScalarAttributeType.S)
                                     .build()
        );

        return attributeDefinitions;
    }

    private static ProvisionedThroughput provisionedThroughputForLocalDatabase() {
        // not sure if provisioned throughput plays any role in Local databases.
        return ProvisionedThroughput.builder()
            .readCapacityUnits(CAPACITY_DOES_NOT_MATTER)
            .writeCapacityUnits(CAPACITY_DOES_NOT_MATTER)
            .build();
    }

    private void assertThatTableKeySchemaContainsBothKeys(List<KeySchemaElement> tableKeySchema) {
        assertThat(tableKeySchema.toString(), containsString(PRIMARY_KEY_HASH_KEY));
        assertThat(tableKeySchema.toString(), containsString(PRIMARY_KEY_RANGE_KEY));
    }

    private DynamoDbClient createLocalDynamoDbMock() {
        return DynamoDBEmbedded.create().dynamoDbClient();
    }


}
