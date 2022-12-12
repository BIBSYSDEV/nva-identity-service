package no.unit.nva.customer.model;

import static no.unit.nva.customer.testing.CustomerDataGenerator.createSampleCustomerDao;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SecretManagerDoiAgentDaoTest {

    @Test
    void fromJson() {
        var customerDao = createSampleCustomerDao();
        var doiAgentDao = customerDao.getDoiAgent();
        var expected = new SecretManagerDoiAgentDao.Builder()
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
}