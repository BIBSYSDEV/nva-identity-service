package no.unit.nva.database;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.ENVIRONMENT;
import static no.unit.useraccessservice.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.database.IdentityService.Constants;
import no.unit.nva.useraccessservice.dao.ClientDao;
import no.unit.nva.useraccessservice.model.ClientDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class ExternalClientService extends DatabaseSubService {

    public static final String CLIENT_NOT_FOUND_MESSAGE = "Could not find client: ";
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");

    private final DynamoDbTable<ClientDao> table;

    public ExternalClientService(DynamoDbClient dynamoClient) {
        super(dynamoClient);
        this.table = this.client.table(Constants.USERS_AND_ROLES_TABLE, ClientDao.TABLE_SCHEMA);
    }

    public void createNewExternalClient(String clientId,
                                                                URI customer) throws BadRequestException {



        var clientDto = ClientDto.newBuilder()
                            .withClientId(clientId)
                            .withCustomer(customer)
                            .build();

        addClientToDB(clientDto);
    }


    public void addClientToDB(ClientDto clientDto) {
        table.putItem(ClientDao.fromClientDto(clientDto));
    }

    /**
     * Fetches a role from the database.
     *
     * @param queryObject the query object containing the rolename.
     * @return the Role that corresponds to the given rolename.
     */
    public ClientDto getClient(ClientDto queryObject) throws NotFoundException {
        return getRoleAsOptional(queryObject)
                   .orElseThrow(() -> new NotFoundException(CLIENT_NOT_FOUND_MESSAGE + queryObject.getClientId()));
    }

    private Optional<ClientDto> getRoleAsOptional(ClientDto queryObject) {
        return Optional.ofNullable(attemptFetchClient(queryObject));
    }

    private ClientDto attemptFetchClient(ClientDto queryObject) {
        ClientDao clientDao = Try.of(queryObject)
                                  .map(ClientDao::fromClientDto)
                                  .map(this::fetchClientDao)
                                  .orElseThrow(DatabaseSubService::handleError);
        return nonNull(clientDao) ? clientDao.toClientDto() : null;
    }

    protected ClientDao fetchClientDao(ClientDao queryObject) {
        return table.getItem(queryObject);
    }

    @JacocoGenerated
    public static ExternalClientService defaultExternalClientService() {

        return new ExternalClientService(DEFAULT_DYNAMO_CLIENT);
    }
}
