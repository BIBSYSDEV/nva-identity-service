package no.unit.nva.database;

import static no.unit.nva.database.ExternalClientService.CLIENT_NOT_FOUND_MESSAGE;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

class ExternalClientServiceTest extends LocalIdentityService {
    public static final String CLIENT_ID = "id1";
    private ExternalClientService service;

    @BeforeEach
    public void setup()  {
        service = new ExternalClientService(initializeTestDatabase());
    }

    @Test
    void shouldInsertClientToDb()
        throws InvalidEntryInternalException, NotFoundException,
               URISyntaxException, BadRequestException {


        var clientDto = ClientDto.newBuilder()
                                     .withClientId(CLIENT_ID)
                                     .withCustomer(new URI("https://example.org/customer"))
                                     .withCristin(new URI("https://example.org/cristin"))
                                     .withOwner("owner")
                                     .build();

        service.createNewExternalClient(clientDto);
        ClientDto clientQuery =
            ClientDto.newBuilder().withClientId(CLIENT_ID).build();

        ClientDto savedClient = service.getClient(clientQuery);

        assertThat(savedClient, doesNotHaveEmptyValues());
        assertThat(savedClient.getClientId(), is(equalTo(savedClient.getClientId())));
    }


    @Test
    void throwsNotFoundExceptionWhenClientDoesNotExist() throws InvalidEntryInternalException {
        ClientDto clientQuery =
            ClientDto.newBuilder().withClientId(randomString()).build();

        Executable action = () -> service.getClient(clientQuery);

        NotFoundException exception = assertThrows(NotFoundException.class, action);
        assertThat(exception.getMessage(), containsString(CLIENT_NOT_FOUND_MESSAGE));
    }

}