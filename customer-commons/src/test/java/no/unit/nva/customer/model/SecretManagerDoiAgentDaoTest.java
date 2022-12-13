package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

class SecretManagerDoiAgentDaoTest {

    @Test
    void fromJsonSuccessful() {
        var customerDao = createSampleCustomerDao();
        var doiAgentDao = customerDao.getDoiAgent();
        var expected = SecretManagerDoiAgentDao.builder()
                            .withCustomerId(customerDao.getCustomerOf())
                            .withPrefix(doiAgentDao.getPrefix())
                            .withUrl(doiAgentDao.getUrl())
                            .withUsername(doiAgentDao.getUsername())
                            .withPassword(randomString())
                            .build();
        var json = expected.toString();
        var actual = attempt(() -> SecretManagerDoiAgentDao.fromJson(json)).get();

        assertEquals(expected, actual);

    }

    @Test
    void fromJsonFailing() {

        var json = randomString();
        var exception = assertThrows(BadRequestException.class, () -> SecretManagerDoiAgentDao.fromJson(json));

        assertEquals(BadRequestException.class,exception.getClass());

    }

    @Test
    void stupidJacocoTestForCoverageThisIsAllReadyTestedElsewhere() {
        var customerDto = createSampleCustomerDao().toCustomerDto();
        var doiAgentDto = customerDto.getDoiAgent();
        var secretManagerDoiAgentDao = new SecretManagerDoiAgentDao(randomUri(),doiAgentDto);

        assertNotNull(secretManagerDoiAgentDao);
    }

}