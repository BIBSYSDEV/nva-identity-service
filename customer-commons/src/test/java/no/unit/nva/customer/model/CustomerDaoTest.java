package no.unit.nva.customer.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import no.unit.nva.customer.ObjectMapperConfig;
import org.junit.jupiter.api.Test;

class CustomerDaoTest {

    @Test
    void daoCanBeDeserializedWhenJsonDoesNotIncludeType() throws JsonProcessingException {
        CustomerDao someDao = sampleCustomerDao();
        ObjectNode json = ObjectMapperConfig.objectMapper.convertValue(someDao, ObjectNode.class);
        json.remove("type");
        CustomerDao deserialized = ObjectMapperConfig.objectMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    @Test
    void daoIsSerializedWithType() throws JsonProcessingException {
        CustomerDao someDao = sampleCustomerDao();
        ObjectNode json = ObjectMapperConfig.objectMapper.convertValue(someDao, ObjectNode.class);
        assertThat(json.has("type"), is((true)));

        CustomerDao deserialized = ObjectMapperConfig.objectMapper.readValue(json.toString(), CustomerDao.class);
        assertThat(deserialized, is(equalTo(someDao)));
    }

    private CustomerDao sampleCustomerDao() {
        return CustomerDao.builder()
            .withArchiveName("someName")
            .withIdentifier(UUID.randomUUID())
            .build();
    }
}