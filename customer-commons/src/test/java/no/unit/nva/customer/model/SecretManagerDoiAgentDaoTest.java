package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomIdentifier;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomDoiAgent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import java.net.URI;

class SecretManagerDoiAgentDaoTest {

    private static final String DOIAGENT = "/doiagent";

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
    void shouldMergeDoiAgentWithoutLoosingInformation() {
        var doiAgentDto = randomDoiAgentDto();
        var secretManagerDoiAgentDao = new SecretManagerDoiAgentDao(doiAgentDto);

        secretManagerDoiAgentDao.merge(doiAgentDto);

        assertNotNull(secretManagerDoiAgentDao);
        assertEquals(secretManagerDoiAgentDao.toDoiAgentDto(), doiAgentDto);
    }

    @Test
    void shouldMergeSecretManagerDoiAgentWithoutChangingCustomer() {
        var doiAgentDto = randomDoiAgentDto();
        var smDoiAgent = new SecretManagerDoiAgentDao(doiAgentDto);
        var smDoiAgent2 = new SecretManagerDoiAgentDao(randomDoiAgentDto());

        smDoiAgent2.merge(smDoiAgent);

        assertEquals(smDoiAgent.toDoiAgentDto(), doiAgentDto);
        assertNotEquals(smDoiAgent.getCustomerId(),smDoiAgent2.getCustomerId());
    }

    private CustomerDto.DoiAgentDto randomDoiAgentDto() {
        return new CustomerDto.DoiAgentDto(randomDoiAgent(randomString()))
            .addPassword(randomString())
            .addId(URI.create(randomUri() + DOIAGENT));
    }


}