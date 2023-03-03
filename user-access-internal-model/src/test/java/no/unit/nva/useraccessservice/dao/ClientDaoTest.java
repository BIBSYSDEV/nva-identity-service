package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.attempt.Try;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientDaoTest {

    public static final String SOME_CLIENT_ID = randomString();
    public static final URI SOME_ORG = randomCristinOrgId();
    private static final Javers JAVERS = JaversBuilder.javers().build();

    private ClientDao clientDao;
    private ClientDao sampleClient;

    @BeforeEach
    public void init() {
        clientDao = new ClientDao();
        sampleClient = ClientDao.newBuilder().withClientId(SOME_CLIENT_ID).withCustomer(SOME_ORG).build();
    }

    @Test
    void builderShouldSetTheHashKeyBasedOnClientId()  {
        sampleClient.setPrimaryKeyHashKey("SomeOtherHashKey");
        String expectedHashKey = String.join(ClientDao.FIELD_DELIMITER, ClientDao.TYPE_VALUE, SOME_CLIENT_ID);
        assertThat(sampleClient.getPrimaryKeyHashKey(), is(equalTo(expectedHashKey)));
    }


    @Test
    void setUsernameShouldAddUsernameToUserObject() {
        clientDao.setClientTd(SOME_CLIENT_ID);
        assertThat(clientDao.getClientTd(), is(equalTo(SOME_CLIENT_ID)));
    }

    @Test
    void getUsernameShouldGetTheSetUsernameToUserObject() {
        assertThat(clientDao.getClientTd(), is(nullValue()));
        clientDao.setClientTd(SOME_CLIENT_ID);
        assertThat(clientDao.getClientTd(), is(equalTo(SOME_CLIENT_ID)));
    }

    @Test
    void getTypeShouldReturnConstantTypeValue() {
        assertThat(clientDao.getType(), is(equalTo(ClientDao.TYPE_VALUE)));
    }

    @Test
    void setTypeShouldNotAcceptWrongTypeValues() {
        String illegalType = "NotExpectedType";
        var exception = assertThrows(BadRequestException.class, () -> clientDao.setType(illegalType));
        assertThat(exception.getMessage(), allOf(containsString(illegalType), containsString(ClientDao.TYPE_VALUE)));
    }

    @Test
    void getHashKeyKeyShouldReturnTypeAndUsernameConcatenation() {
        String expectedHashKey = String.join(ClientDao.FIELD_DELIMITER, ClientDao.TYPE_VALUE, SOME_CLIENT_ID);
        assertThat(sampleClient.getPrimaryKeyHashKey(), is(equalTo(expectedHashKey)));
    }

    @Test
    void shouldReturnCopyWithFilledInFields() {
        ClientDao originalUser = randomClientDb();
        ClientDao copy = originalUser.copy().build();
        assertThat(copy, is(equalTo(originalUser)));

        assertThat(copy, is(not(sameInstance(originalUser))));
    }

    @Test
    void shouldConvertToDtoAndBackWithoutInformationLoss() {
        ClientDao originalUser = randomClientDb();
        ClientDao converted = Try.of(originalUser)
            .map(ClientDao::toClientDto)
            .map(ClientDao::fromClientDto)
            .orElseThrow();

        assertThat(originalUser, is(equalTo(converted)));
        Diff diff = JAVERS.compare(originalUser, converted);
        assertThat(diff.prettyPrint(), diff.hasChanges(), is(false));
        assertThat(converted, doesNotHaveEmptyValues());
    }

    @Test
    void shouldCopyWithoutInformationLoss() {
        var source = randomClientDb();
        assertThat(source,doesNotHaveEmptyValues());
        var copy = source.copy().build();
        assertThat(copy,doesNotHaveEmptyValues());
        assertThat(copy,is(equalTo(source)));
    }

    private ClientDao randomClientDb() {
        ClientDao randomUser = ClientDao.newBuilder()
            .withClientId(randomString())
            .withCustomer(randomCristinOrgId())
            .build();
        assertThat(randomUser, doesNotHaveEmptyValues());
        return randomUser;
    }

}