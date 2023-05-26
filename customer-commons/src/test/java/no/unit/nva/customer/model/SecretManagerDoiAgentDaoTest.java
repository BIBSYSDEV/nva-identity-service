package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class SecretManagerDoiAgentDaoTest {

    @Test
    void shouldDeserialize() {
        var doiAgentDto = createSampleCustomerDao().getDoiAgent().toDoiAgentDto()
                              .addPassword(randomString())
                              .addIdByIdentifier(randomIdentifier());
        var expected = new SecretManagerDoiAgentDao(doiAgentDto);
        var json = expected.toString();
        var actual = attempt(() -> SecretManagerDoiAgentDao.fromJson(json)).get();

        assertEquals(expected, actual);
        assertEquals(expected.toDoiAgentDto(), doiAgentDto);
    }

    @Test
    void shouldFailDeserialize() {

        var json = randomString();
        var exception = assertThrows(BadRequestException.class, () -> SecretManagerDoiAgentDao.fromJson(json));

        assertEquals(BadRequestException.class, exception.getClass());
    }

    @Test
    void shouldUpdateDoiAgent() {
        var doiAgentDto =
            createSampleCustomerDao()
                .toCustomerDto()
                .getDoiAgent()
                .addPassword(randomString());
        var secretManagerDoiAgentDao = new SecretManagerDoiAgentDao(doiAgentDto);

        secretManagerDoiAgentDao.merge(doiAgentDto);

        assertEquals(secretManagerDoiAgentDao.toDoiAgentDto(), doiAgentDto);
        assertNotNull(secretManagerDoiAgentDao);
    }

    @Test
    void shouldUpdateSecretManagerDoiAgent() {
        var doiAgentDto =
            createSampleCustomerDao()
                .toCustomerDto()
                .getDoiAgent()
                .addPassword(randomString());
        var secretManagerDoiAgentDao = new SecretManagerDoiAgentDao(doiAgentDto);
        doiAgentDto.setPassword(randomString());
        var secretManagerDoiAgentDao2 = new SecretManagerDoiAgentDao(doiAgentDto);

        secretManagerDoiAgentDao.merge(secretManagerDoiAgentDao2);

        assertEquals(secretManagerDoiAgentDao.toDoiAgentDto(), doiAgentDto);
        assertNotNull(secretManagerDoiAgentDao);
    }
}